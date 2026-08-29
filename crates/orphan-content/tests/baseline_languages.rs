use orphan_content::{LanguagePack, LANGUAGES, NARRATIVE_SLOT_COUNT, UI_SLOT_COUNT};
use sha2::{Digest, Sha256};
use std::fs;
use std::io::{Cursor, Read};
use std::path::Path;
use zip::ZipArchive;

const BASELINE_SHA256: &str = "ca9a874ce7c7fb11ed444701b65e967919d3d7c0bcf73f80a9fcf825fb6a4be7";

fn hex(bytes: &[u8]) -> String {
    format!("{:x}", Sha256::digest(bytes))
}

fn find_baseline() -> Vec<u8> {
    let originals = Path::new(env!("CARGO_MANIFEST_DIR")).join("../../_originals");
    assert!(
        originals.is_dir() && !originals.is_symlink(),
        "_originals is absent"
    );
    let mut paths: Vec<_> = fs::read_dir(originals)
        .unwrap()
        .map(|entry| entry.unwrap().path())
        .filter(|path| path.is_file())
        .collect();
    paths.sort();
    for path in paths {
        let outer = fs::read(path).unwrap();
        if hex(&outer) == BASELINE_SHA256 {
            return outer;
        }
        let Ok(mut archive) = ZipArchive::new(Cursor::new(&outer)) else {
            continue;
        };
        for index in 0..archive.len() {
            let mut member = archive.by_index(index).unwrap();
            let mut bytes = Vec::new();
            member.read_to_end(&mut bytes).unwrap();
            if hex(&bytes) == BASELINE_SHA256 {
                return bytes;
            }
        }
    }
    panic!("selected baseline payload is absent");
}

#[test]
fn all_content_proven_baseline_languages_are_integrated() {
    let bytes = find_baseline();
    let mut jar = ZipArchive::new(Cursor::new(bytes)).unwrap();
    for descriptor in &LANGUAGES {
        let mut ui = Vec::new();
        jar.by_name(descriptor.ui_member)
            .unwrap()
            .read_to_end(&mut ui)
            .unwrap();
        let mut narrative = Vec::new();
        jar.by_name(descriptor.narrative_member)
            .unwrap()
            .read_to_end(&mut narrative)
            .unwrap();
        assert_eq!(hex(&ui), descriptor.ui_sha256);
        assert_eq!(hex(&narrative), descriptor.narrative_sha256);
        let pack = LanguagePack::decode(descriptor, &ui, &narrative).unwrap();
        assert_eq!(pack.ui.len(), UI_SLOT_COUNT);
        assert_eq!(pack.narrative.strings.len(), NARRATIVE_SLOT_COUNT);
    }
}
