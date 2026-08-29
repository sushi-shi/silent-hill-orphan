use orphan_formats::{line_table, LanTable};
use std::collections::BTreeSet;
use std::fs;
use std::io::{Cursor, Read};
use std::path::Path;
use zip::ZipArchive;

#[derive(Default)]
struct Stats {
    jars: usize,
    lan_tables: usize,
    ui_tables: usize,
    strings: usize,
    locale_codes: BTreeSet<String>,
}

fn scan_archive(bytes: &[u8], label: &str, depth: usize, stats: &mut Stats) {
    assert!(depth <= 2, "unexpected archive nesting below {label}");
    let Ok(mut archive) = ZipArchive::new(Cursor::new(bytes)) else {
        return;
    };
    let is_jar = (0..archive.len()).any(|index| {
        archive
            .by_index(index)
            .map(|entry| entry.name().ends_with(".class"))
            .unwrap_or(false)
    });
    if is_jar {
        stats.jars += 1;
    }

    for index in 0..archive.len() {
        let mut entry = archive.by_index(index).unwrap();
        if entry.is_dir() {
            continue;
        }
        let name = entry.name().to_owned();
        let mut data = Vec::new();
        entry.read_to_end(&mut data).unwrap();
        let lower = name.to_ascii_lowercase();
        if lower.ends_with(".jar") || lower.ends_with(".zip") {
            scan_archive(&data, &format!("{label}::{name}"), depth + 1, stats);
        } else if is_jar && lower.starts_with("sh/lan/") && lower.ends_with(".lan") {
            let table =
                LanTable::decode(&data).unwrap_or_else(|error| panic!("{label}::{name}: {error}"));
            assert!(
                !table.strings.is_empty(),
                "{label}::{name}: empty language table"
            );
            stats.lan_tables += 1;
            stats.strings += table.strings.len();
            stats.locale_codes.insert(
                Path::new(&lower)
                    .file_stem()
                    .unwrap()
                    .to_string_lossy()
                    .into_owned(),
            );
        } else if is_jar && lower.starts_with("localization/") && lower.ends_with(".properties") {
            let count = line_table(&data).count();
            assert!(count > 1, "{label}::{name}: vacuous UI table");
            stats.ui_tables += 1;
            stats.locale_codes.insert(
                Path::new(&lower)
                    .file_stem()
                    .unwrap()
                    .to_string_lossy()
                    .into_owned(),
            );
        }
    }
}

#[test]
fn every_corpus_language_blob_has_valid_bounded_framing() {
    let repo = Path::new(env!("CARGO_MANIFEST_DIR")).join("../..");
    let originals = repo.join("_originals");
    assert!(
        originals.is_dir() && !originals.is_symlink(),
        "_originals is absent"
    );

    let mut paths: Vec<_> = fs::read_dir(&originals)
        .unwrap()
        .map(|entry| entry.unwrap().path())
        .filter(|path| path.is_file())
        .collect();
    paths.sort();
    let mut stats = Stats::default();
    for path in paths {
        let bytes = fs::read(&path).unwrap();
        scan_archive(&bytes, &path.display().to_string(), 0, &mut stats);
    }

    assert!(
        stats.jars >= 115,
        "parsed only {} JAR occurrences",
        stats.jars
    );
    assert!(
        stats.lan_tables >= 100,
        "parsed only {} .lan tables",
        stats.lan_tables
    );
    assert!(
        stats.ui_tables >= 100,
        "parsed only {} UI tables",
        stats.ui_tables
    );
    assert!(
        stats.strings >= 10_000,
        "parsed only {} narrative strings",
        stats.strings
    );
    assert_eq!(
        stats.locale_codes,
        ["de", "en", "es", "fr", "it", "pt"]
            .into_iter()
            .map(String::from)
            .collect()
    );
}

#[test]
fn malformed_language_records_are_rejected_without_panicking() {
    for bytes in [&[][..], &[0][..], &[0, 1, 0, 2, b'A'][..], &[0, 0, 1][..]] {
        assert!(LanTable::decode(bytes).is_err());
    }
}
