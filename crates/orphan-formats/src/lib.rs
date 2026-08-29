#![no_std]
//! Bounded, allocation-optional codecs for Silent Hill: Orphan resources.
//!
//! Core decoders borrow from byte slices and require neither a filesystem nor
//! an OS. Corpus traversal, JAR access, PNG, and audio belong in `std` tooling.

extern crate alloc;

#[cfg(feature = "std")]
extern crate std;

use alloc::vec::Vec;
use core::fmt;

pub mod ink;

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum FormatError {
    Truncated {
        at: usize,
        needed: usize,
        remaining: usize,
    },
    Invalid {
        at: usize,
        reason: &'static str,
    },
    TrailingBytes {
        at: usize,
        remaining: usize,
    },
}

fn validate_modified_utf(bytes: &[u8], base: usize) -> Result<(), FormatError> {
    let mut position = 0;
    while position < bytes.len() {
        let first = bytes[position];
        let width = if first & 0x80 == 0 {
            // DataInputStream.readUTF accepts raw 0 even though writeUTF emits
            // it as C0 80. Match the decoder, not the stricter format prose.
            1
        } else if first & 0xe0 == 0xc0 {
            2
        } else if first & 0xf0 == 0xe0 {
            3
        } else {
            return Err(FormatError::Invalid {
                at: base + position,
                reason: "invalid modified UTF-8 leading byte",
            });
        };
        if position + width > bytes.len() {
            return Err(FormatError::Invalid {
                at: base + position,
                reason: "truncated modified UTF-8 sequence",
            });
        }
        if bytes[position + 1..position + width]
            .iter()
            .any(|byte| byte & 0xc0 != 0x80)
        {
            return Err(FormatError::Invalid {
                at: base + position,
                reason: "invalid modified UTF-8 continuation byte",
            });
        }
        position += width;
    }
    Ok(())
}

impl fmt::Display for FormatError {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Truncated {
                at,
                needed,
                remaining,
            } => write!(
                formatter,
                "truncated at {at}: need {needed} byte(s), {remaining} remain"
            ),
            Self::Invalid { at, reason } => write!(formatter, "invalid data at {at}: {reason}"),
            Self::TrailingBytes { at, remaining } => {
                write!(formatter, "{remaining} trailing byte(s) at {at}")
            }
        }
    }
}

#[cfg(feature = "std")]
impl std::error::Error for FormatError {}

#[derive(Clone, Debug)]
pub struct Reader<'a> {
    bytes: &'a [u8],
    position: usize,
}

impl<'a> Reader<'a> {
    pub const fn new(bytes: &'a [u8]) -> Self {
        Self { bytes, position: 0 }
    }

    pub const fn position(&self) -> usize {
        self.position
    }

    pub const fn remaining(&self) -> usize {
        self.bytes.len() - self.position
    }

    pub const fn is_empty(&self) -> bool {
        self.remaining() == 0
    }

    pub fn bytes(&mut self, count: usize) -> Result<&'a [u8], FormatError> {
        let end = self
            .position
            .checked_add(count)
            .ok_or(FormatError::Invalid {
                at: self.position,
                reason: "offset overflow",
            })?;
        if end > self.bytes.len() {
            return Err(FormatError::Truncated {
                at: self.position,
                needed: count,
                remaining: self.remaining(),
            });
        }
        let result = &self.bytes[self.position..end];
        self.position = end;
        Ok(result)
    }

    pub fn u8(&mut self) -> Result<u8, FormatError> {
        Ok(self.bytes(1)?[0])
    }

    pub fn u16_be(&mut self) -> Result<u16, FormatError> {
        let bytes = self.bytes(2)?;
        Ok(u16::from_be_bytes([bytes[0], bytes[1]]))
    }

    pub fn i16_be(&mut self) -> Result<i16, FormatError> {
        let bytes = self.bytes(2)?;
        Ok(i16::from_be_bytes([bytes[0], bytes[1]]))
    }

    pub fn u32_be(&mut self) -> Result<u32, FormatError> {
        let bytes = self.bytes(4)?;
        Ok(u32::from_be_bytes([bytes[0], bytes[1], bytes[2], bytes[3]]))
    }

    pub fn finish(self) -> Result<(), FormatError> {
        if self.is_empty() {
            Ok(())
        } else {
            Err(FormatError::TrailingBytes {
                at: self.position,
                remaining: self.remaining(),
            })
        }
    }
}

/// Borrowed strings from one `sh/lan/*.lan` table.
#[derive(Clone, Debug, Eq, PartialEq)]
pub struct LanTable<'a> {
    pub strings: Vec<&'a [u8]>,
    /// Two archived Spanish tables append one LF after the declared records.
    /// Retail stops after `count`; preserving the tolerated byte makes the
    /// anomaly explicit without accepting arbitrary garbage.
    pub trailing_line_feed: bool,
}

/// One member borrowed from an INK `chunks/*.bin` resource archive.
#[derive(Clone, Debug, Eq, PartialEq)]
pub struct ChunkEntry<'a> {
    pub name: &'a [u8],
    pub data: &'a [u8],
}

/// The compact archive framing consumed by `M.resourceGetNamesAndData` and
/// `M.resourceGetFromBytes`.
#[derive(Clone, Debug, Eq, PartialEq)]
pub struct ChunkArchive<'a> {
    pub entries: Vec<ChunkEntry<'a>>,
}

impl<'a> ChunkArchive<'a> {
    /// Decode `u8 count`, then `count` records of `u8 name_len`, name bytes,
    /// big-endian `u16 data_len`, and exact data bytes.
    pub fn decode(bytes: &'a [u8]) -> Result<Self, FormatError> {
        let mut reader = Reader::new(bytes);
        let count = usize::from(reader.u8()?);
        let mut entries = Vec::new();
        entries
            .try_reserve_exact(count)
            .map_err(|_| FormatError::Invalid {
                at: 0,
                reason: "chunk entry count exceeds allocation capacity",
            })?;
        for _ in 0..count {
            let name_length = usize::from(reader.u8()?);
            if name_length == 0 {
                return Err(FormatError::Invalid {
                    at: reader.position() - 1,
                    reason: "chunk member name is empty",
                });
            }
            let name = reader.bytes(name_length)?;
            let data_length = usize::from(reader.u16_be()?);
            let data = reader.bytes(data_length)?;
            entries.push(ChunkEntry { name, data });
        }
        reader.finish()?;
        Ok(Self { entries })
    }

    pub fn get(&self, name: &[u8]) -> Option<&'a [u8]> {
        self.entries
            .iter()
            .find(|entry| entry.name == name)
            .map(|entry| entry.data)
    }
}

impl<'a> LanTable<'a> {
    /// Decode the observed big-endian count/length framing.
    ///
    /// Each record length is the exact encoded byte count. Text encoding is
    /// intentionally left uninterpreted until every language variant is
    /// classified; callers receive the original encoded bytes.
    pub fn decode(bytes: &'a [u8]) -> Result<Self, FormatError> {
        let mut reader = Reader::new(bytes);
        let count = usize::from(reader.u16_be()?);
        let mut strings = Vec::new();
        strings
            .try_reserve_exact(count)
            .map_err(|_| FormatError::Invalid {
                at: 0,
                reason: "string count exceeds allocation capacity",
            })?;
        for _ in 0..count {
            let length = usize::from(reader.u16_be()?);
            let at = reader.position();
            let encoded = reader.bytes(length)?;
            validate_modified_utf(encoded, at)?;
            strings.push(encoded);
        }
        let trailing_line_feed = if reader.remaining() == 1 {
            if reader.bytes(1)? != b"\n" {
                return Err(FormatError::TrailingBytes {
                    at: reader.position() - 1,
                    remaining: 1,
                });
            }
            true
        } else {
            false
        };
        reader.finish()?;
        Ok(Self {
            strings,
            trailing_line_feed,
        })
    }
}

/// Iterate the line-indexed `localization/*.properties` resource as retail did.
///
/// Despite its suffix this is not a Java key/value properties file: semantic
/// identity is the zero-based line number. `M.loadLanguage` treats CR and LF as
/// separators, suppresses empty records (including CRLF's empty half), and only
/// commits a record when it sees a separator. Consequently a non-empty final
/// fragment without CR/LF is ignored; this oddity is intentional fidelity.
pub fn line_table(bytes: &[u8]) -> impl Iterator<Item = &[u8]> {
    let terminated = bytes
        .iter()
        .rposition(|byte| matches!(*byte, b'\r' | b'\n'))
        .map_or(&bytes[..0], |last| &bytes[..=last]);
    terminated
        .split(|byte| matches!(*byte, b'\r' | b'\n'))
        .filter(|line| !line.is_empty())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn reader_rejects_truncation_and_offset_overflow() {
        let mut reader = Reader::new(&[1]);
        assert_eq!(reader.u8(), Ok(1));
        assert!(matches!(
            reader.u16_be(),
            Err(FormatError::Truncated { .. })
        ));

        let mut reader = Reader::new(&[1]);
        assert_eq!(reader.u8(), Ok(1));
        assert!(matches!(
            reader.bytes(usize::MAX),
            Err(FormatError::Invalid {
                reason: "offset overflow",
                ..
            })
        ));
    }

    #[test]
    fn lan_table_borrows_exact_length_records() {
        let table = LanTable::decode(&[0, 2, 0, 1, b'A', 0, 2, b'B', b'C']).unwrap();
        assert_eq!(table.strings, [b"A".as_slice(), b"BC".as_slice()]);
        assert!(!table.trailing_line_feed);
    }

    #[test]
    fn lan_table_rejects_truncation_and_trailing_data() {
        assert!(matches!(
            LanTable::decode(&[0, 1, 0, 2, b'A']),
            Err(FormatError::Truncated { .. })
        ));
        assert!(matches!(
            LanTable::decode(&[0, 0, 1]),
            Err(FormatError::TrailingBytes { .. })
        ));
        let table = LanTable::decode(&[0, 1, 0, 1, b'A', b'\n']).unwrap();
        assert!(table.trailing_line_feed);
        assert!(LanTable::decode(&[0, 1, 0, 1, 0]).is_ok());
        assert!(matches!(
            LanTable::decode(&[0, 1, 0, 1, 0x80]),
            Err(FormatError::Invalid {
                reason: "invalid modified UTF-8 leading byte",
                ..
            })
        ));
    }

    #[test]
    fn line_table_matches_retail_separator_behavior() {
        let lines: Vec<_> = line_table(b"English\r\nEspa\xc3\xb1ol\n\nignored").collect();
        assert_eq!(lines, [b"English".as_slice(), b"Espa\xc3\xb1ol".as_slice()]);
    }

    #[test]
    fn chunk_archive_borrows_names_and_payloads() {
        let archive =
            ChunkArchive::decode(&[2, 1, b'a', 0, 2, 10, 11, 2, b'b', b'c', 0, 0]).unwrap();
        assert_eq!(archive.get(b"a"), Some([10, 11].as_slice()));
        assert_eq!(archive.get(b"bc"), Some([].as_slice()));
        assert_eq!(archive.get(b"missing"), None);
    }

    #[test]
    fn chunk_archive_rejects_truncation_empty_names_and_trailing_data() {
        for bytes in [&[1, 1, b'a', 0, 2, 10][..], &[1, 0, 0, 0][..], &[0, 1][..]] {
            assert!(ChunkArchive::decode(bytes).is_err());
        }
    }
}
