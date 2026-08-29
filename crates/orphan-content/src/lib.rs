#![no_std]
//! Content identities for the six language packs proven to match the baseline.
//!
//! Original bytes are never embedded. A host supplies the two resource slices
//! named by [`LanguageDescriptor`], and this crate validates their slot shape.

extern crate alloc;

#[cfg(feature = "std")]
extern crate std;

use alloc::vec::Vec;
use orphan_formats::{line_table, FormatError, LanTable};

pub const UI_SLOT_COUNT: usize = 185;
pub const NARRATIVE_SLOT_COUNT: usize = 714;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum Language {
    German,
    English,
    Spanish,
    French,
    Italian,
    Portuguese,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct LanguageDescriptor {
    pub language: Language,
    pub code: &'static str,
    pub ui_member: &'static str,
    pub ui_sha256: &'static str,
    pub narrative_member: &'static str,
    pub narrative_sha256: &'static str,
}

pub const LANGUAGES: [LanguageDescriptor; 6] = [
    descriptor(
        Language::German,
        "de",
        "153ba31b8fa926b9f0ffea5d82af950a0d36e35bfee7f896ba2ba4c3ad33ca7b",
        "19b8555613d83a241c5015469e8d82eb82e027f6fb3417423f407f873e1b2405",
    ),
    descriptor(
        Language::English,
        "en",
        "6683bc4ea035b50001c09936455d395f30ecbc3f192bdcb6e82a37ed38d3042e",
        "969cc3c4693ad6be1b610b480a1bdcaa80ad3c35313ba51bde688af28a4d89ea",
    ),
    descriptor(
        Language::Spanish,
        "es",
        "a5ddcd86c1aed9081bc5ea9e8d59aacd7e4dd5997a963cefdaec7547511c5ac1",
        "9ccf699efc1ab7285ff030201fd8be3b7a10f860582252f847c8c2b870385a7f",
    ),
    descriptor(
        Language::French,
        "fr",
        "17684c029b3ec8d80d58370913c60d0c3cc9e619ab2fceae208fb2276b1e609c",
        "f2c5ae4db0ec26927de64615d797b5bb1f379dec46d808cdc36767ea14e971f6",
    ),
    descriptor(
        Language::Italian,
        "it",
        "48bd25811e134389c1af39bcd3073a447e99f380d7b3a50ab0fcc628e66da43d",
        "38663514959cf4f538e1e0b7a326e49186a480ab883068941c136669aa43f600",
    ),
    descriptor(
        Language::Portuguese,
        "pt",
        "824776c7f4fcb8e9f9b8059e0279ff4842b4dc29d5e7e6fb8e9fabde388d7209",
        "c6e9a159dd0716fed7d18d24c29c451b25c6c434ca3094933f20692674fa9a22",
    ),
];

const fn descriptor(
    language: Language,
    code: &'static str,
    ui_sha256: &'static str,
    narrative_sha256: &'static str,
) -> LanguageDescriptor {
    LanguageDescriptor {
        language,
        code,
        ui_member: match language {
            Language::German => "localization/de.properties",
            Language::English => "localization/en.properties",
            Language::Spanish => "localization/es.properties",
            Language::French => "localization/fr.properties",
            Language::Italian => "localization/it.properties",
            Language::Portuguese => "localization/pt.properties",
        },
        ui_sha256,
        narrative_member: match language {
            Language::German => "sh/lan/de.lan",
            Language::English => "sh/lan/en.lan",
            Language::Spanish => "sh/lan/es.lan",
            Language::French => "sh/lan/fr.lan",
            Language::Italian => "sh/lan/it.lan",
            Language::Portuguese => "sh/lan/pt.lan",
        },
        narrative_sha256,
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum LanguageError {
    Narrative(FormatError),
    UiEncoding { slot: usize },
    UiSlotCount { actual: usize },
    NarrativeSlotCount { actual: usize },
}

pub struct LanguagePack<'a> {
    pub descriptor: &'static LanguageDescriptor,
    pub ui: Vec<&'a [u8]>,
    pub narrative: LanTable<'a>,
}

impl<'a> LanguagePack<'a> {
    pub fn decode(
        descriptor: &'static LanguageDescriptor,
        ui_bytes: &'a [u8],
        narrative_bytes: &'a [u8],
    ) -> Result<Self, LanguageError> {
        let ui: Vec<_> = line_table(ui_bytes).collect();
        if ui.len() != UI_SLOT_COUNT {
            return Err(LanguageError::UiSlotCount { actual: ui.len() });
        }
        if let Some(slot) = ui
            .iter()
            .position(|value| core::str::from_utf8(value).is_err())
        {
            return Err(LanguageError::UiEncoding { slot });
        }
        let narrative = LanTable::decode(narrative_bytes).map_err(LanguageError::Narrative)?;
        if narrative.strings.len() != NARRATIVE_SLOT_COUNT {
            return Err(LanguageError::NarrativeSlotCount {
                actual: narrative.strings.len(),
            });
        }
        Ok(Self {
            descriptor,
            ui,
            narrative,
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn rejects_wrong_slot_shapes() {
        let descriptor = &LANGUAGES[1];
        assert!(matches!(
            LanguagePack::decode(descriptor, b"only one\n", &[0, 0]),
            Err(LanguageError::UiSlotCount { actual: 1 })
        ));
    }
}
