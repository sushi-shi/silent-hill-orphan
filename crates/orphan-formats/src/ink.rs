//! Bounded decoders for INK room and script members stored inside chunks.

use alloc::vec::Vec;

use crate::{validate_modified_utf, FormatError, Reader};

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct StringTable<'a> {
    /// Java `DataInputStream.readUTF` payload bytes, without each `u16` length.
    pub encoded: Vec<&'a [u8]>,
}

impl<'a> StringTable<'a> {
    fn decode(reader: &mut Reader<'a>) -> Result<Self, FormatError> {
        let count = usize::from(reader.u8()?);
        let mut encoded = Vec::new();
        encoded
            .try_reserve_exact(count)
            .map_err(|_| FormatError::Invalid {
                at: reader.position() - 1,
                reason: "string-table count exceeds allocation capacity",
            })?;
        for _ in 0..count {
            let length = usize::from(reader.u16_be()?);
            let at = reader.position();
            let value = reader.bytes(length)?;
            validate_modified_utf(value, at)?;
            encoded.push(value);
        }
        Ok(Self { encoded })
    }

    fn checked_index(&self, reader: &mut Reader<'a>, allow_zero: bool) -> Result<u8, FormatError> {
        let at = reader.position();
        let index = reader.u8()?;
        if (index == 0 && allow_zero) || (index > 0 && usize::from(index) <= self.encoded.len()) {
            Ok(index)
        } else {
            Err(FormatError::Invalid {
                at,
                reason: "string-table index is out of range",
            })
        }
    }
}

fn indexed_references<'a>(
    reader: &mut Reader<'a>,
    strings: &StringTable<'a>,
) -> Result<Vec<u8>, FormatError> {
    let count = usize::from(reader.u8()?);
    let mut result = Vec::new();
    result
        .try_reserve_exact(count)
        .map_err(|_| FormatError::Invalid {
            at: reader.position() - 1,
            reason: "reference count exceeds allocation capacity",
        })?;
    for _ in 0..count {
        result.push(strings.checked_index(reader, false)?);
    }
    Ok(result)
}

fn integer_references(reader: &mut Reader<'_>) -> Result<Vec<u16>, FormatError> {
    let count = usize::from(reader.u8()?);
    let mut result = Vec::new();
    result
        .try_reserve_exact(count)
        .map_err(|_| FormatError::Invalid {
            at: reader.position() - 1,
            reason: "reference count exceeds allocation capacity",
        })?;
    for _ in 0..count {
        result.push(reader.u16_be()?);
    }
    Ok(result)
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct ScriptReferences {
    pub adjacent_rooms: Vec<u8>,
    pub scripts: Vec<u8>,
    pub image_names: Vec<u8>,
    pub image_ids: Vec<u16>,
    pub sounds: Vec<u8>,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum GfxReference {
    None,
    String(u8),
    Integer(u16),
}

fn gfx_reference<'a>(
    reader: &mut Reader<'a>,
    strings: &StringTable<'a>,
) -> Result<GfxReference, FormatError> {
    let at = reader.position();
    match reader.u8()? {
        0 => Ok(GfxReference::None),
        1 => Ok(GfxReference::String(strings.checked_index(reader, false)?)),
        2 => Ok(GfxReference::Integer(reader.u16_be()?)),
        _ => Err(FormatError::Invalid {
            at,
            reason: "unknown graphics reference type",
        }),
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct ScriptResource<'a> {
    pub strings: StringTable<'a>,
    pub references: ScriptReferences,
    pub gfx: GfxReference,
    pub events: Vec<(u8, u16)>,
    pub bytecode: &'a [u8],
}

impl<'a> ScriptResource<'a> {
    pub fn decode(bytes: &'a [u8]) -> Result<Self, FormatError> {
        let mut reader = Reader::new(bytes);
        let strings = StringTable::decode(&mut reader)?;
        let references = ScriptReferences {
            adjacent_rooms: indexed_references(&mut reader, &strings)?,
            scripts: indexed_references(&mut reader, &strings)?,
            image_names: indexed_references(&mut reader, &strings)?,
            image_ids: integer_references(&mut reader)?,
            sounds: indexed_references(&mut reader, &strings)?,
        };
        let gfx = gfx_reference(&mut reader, &strings)?;
        let event_count = usize::from(reader.u8()?);
        let mut events = Vec::new();
        events
            .try_reserve_exact(event_count)
            .map_err(|_| FormatError::Invalid {
                at: reader.position() - 1,
                reason: "event count exceeds allocation capacity",
            })?;
        for _ in 0..event_count {
            let at = reader.position();
            let event = reader.u8()?;
            if event >= 57 {
                return Err(FormatError::Invalid {
                    at,
                    reason: "script event id exceeds the 57-slot table",
                });
            }
            events.push((event, reader.u16_be()?));
        }
        let bytecode_length = usize::from(reader.u16_be()?);
        let bytecode = reader.bytes(bytecode_length)?;
        reader.finish()?;
        if events
            .iter()
            .any(|(_, offset)| usize::from(*offset) >= bytecode.len())
        {
            return Err(FormatError::Invalid {
                at: 0,
                reason: "script event offset is outside bytecode",
            });
        }
        Ok(Self {
            strings,
            references,
            gfx,
            events,
            bytecode,
        })
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct RoomReferences {
    pub scripts: Vec<u8>,
    pub image_names: Vec<(u8, u8)>,
    pub image_ids: Vec<(u16, u8)>,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum RoomObject {
    Graphic {
        x: i16,
        y: i16,
        transform: u8,
        gfx: GfxReference,
        script: u8,
    },
    Zone {
        kind: u8,
        x: i16,
        y: i16,
        width: u8,
        height: u8,
        script: u8,
    },
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct RoomResource<'a> {
    pub strings: StringTable<'a>,
    pub references: RoomReferences,
    pub objects: Vec<RoomObject>,
}

impl<'a> RoomResource<'a> {
    pub fn decode(bytes: &'a [u8]) -> Result<Self, FormatError> {
        let mut reader = Reader::new(bytes);
        let strings = StringTable::decode(&mut reader)?;
        let scripts = indexed_references(&mut reader, &strings)?;
        let image_name_count = usize::from(reader.u8()?);
        let mut image_names = Vec::new();
        for _ in 0..image_name_count {
            image_names.push((strings.checked_index(&mut reader, false)?, reader.u8()?));
        }
        let image_id_count = usize::from(reader.u8()?);
        let mut image_ids = Vec::new();
        for _ in 0..image_id_count {
            image_ids.push((reader.u16_be()?, reader.u8()?));
        }
        let references = RoomReferences {
            scripts,
            image_names,
            image_ids,
        };
        let object_count = usize::from(reader.u8()?);
        let mut objects = Vec::new();
        objects
            .try_reserve_exact(object_count)
            .map_err(|_| FormatError::Invalid {
                at: reader.position() - 1,
                reason: "room object count exceeds allocation capacity",
            })?;
        for _ in 0..object_count {
            let at = reader.position();
            let kind = reader.u8()?;
            let object = match kind {
                1 => RoomObject::Graphic {
                    x: reader.i16_be()?,
                    y: reader.i16_be()?,
                    transform: reader.u8()?,
                    gfx: gfx_reference(&mut reader, &strings)?,
                    script: strings.checked_index(&mut reader, true)?,
                },
                2..=6 => RoomObject::Zone {
                    kind,
                    x: reader.i16_be()?,
                    y: reader.i16_be()?,
                    width: reader.u8()?,
                    height: reader.u8()?,
                    script: strings.checked_index(&mut reader, true)?,
                },
                _ => {
                    return Err(FormatError::Invalid {
                        at,
                        reason: "unknown room object type",
                    });
                }
            };
            objects.push(object);
        }
        reader.finish()?;
        Ok(Self {
            strings,
            references,
            objects,
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn minimal_script_and_room_decode() {
        let script = ScriptResource::decode(&[
            1, 0, 1, b'x', // string table
            0, 0, 0, 0, 0, // reference groups
            0, // no gfx
            0, // no events
            0, 0, // empty bytecode
        ])
        .unwrap();
        assert_eq!(script.strings.encoded, [b"x".as_slice()]);

        let room = RoomResource::decode(&[
            1, 0, 1, b'x', // string table
            0, 0, 0, // references
            1, // object count
            2, 0, 1, 0, 2, 3, 4, 0, // zone + absent script
        ])
        .unwrap();
        assert_eq!(room.objects.len(), 1);
    }
}
