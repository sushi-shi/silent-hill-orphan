use orphan_formats::ChunkArchive;
use std::collections::BTreeSet;
use std::fs;
use std::io::{Cursor, Read};
use std::path::Path;
use zip::ZipArchive;

#[derive(Default)]
struct Stats {
    archives: usize,
    entries: usize,
    unique_names: BTreeSet<Vec<u8>>,
}

fn scan(bytes: &[u8], label: &str, depth: usize, stats: &mut Stats) {
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
    for index in 0..archive.len() {
        let mut member = archive.by_index(index).unwrap();
        if member.is_dir() {
            continue;
        }
        let name = member.name().to_owned();
        let mut data = Vec::new();
        member.read_to_end(&mut data).unwrap();
        let lower = name.to_ascii_lowercase();
        if lower.ends_with(".jar") || lower.ends_with(".zip") {
            scan(&data, &format!("{label}::{name}"), depth + 1, stats);
        } else if is_jar && lower.starts_with("chunks/") && lower.ends_with(".bin") {
            let decoded = ChunkArchive::decode(&data)
                .unwrap_or_else(|error| panic!("{label}::{name}: {error}"));
            stats.archives += 1;
            stats.entries += decoded.entries.len();
            for entry in decoded.entries {
                assert!(
                    !entry.name.contains(&0),
                    "{label}::{name}: NUL in member name"
                );
                stats.unique_names.insert(entry.name.to_vec());
            }
        }
    }
}

#[test]
fn every_corpus_chunk_has_valid_bounded_framing() {
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
    let mut stats = Stats::default();
    for path in paths {
        scan(
            &fs::read(&path).unwrap(),
            &path.display().to_string(),
            0,
            &mut stats,
        );
    }
    assert!(
        stats.archives >= 1_000,
        "parsed only {} chunk occurrences",
        stats.archives
    );
    assert!(
        stats.entries >= 10_000,
        "parsed only {} chunk entries",
        stats.entries
    );
    assert!(
        stats.unique_names.len() >= 500,
        "only {} unique member names",
        stats.unique_names.len()
    );
}
