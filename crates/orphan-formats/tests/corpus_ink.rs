use orphan_formats::ink::{RoomResource, ScriptResource};
use orphan_formats::ChunkArchive;
use std::fs;
use std::io::{Cursor, Read};
use std::path::Path;
use zip::ZipArchive;

#[derive(Default)]
struct Stats {
    scripts: usize,
    rooms: usize,
    script_bytes: usize,
    room_objects: usize,
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
            let chunk = ChunkArchive::decode(&data).unwrap();
            for entry in chunk.entries {
                if entry.name.starts_with(b"sh/scr/") && entry.name.ends_with(b".bin") {
                    let script = ScriptResource::decode(entry.data).unwrap_or_else(|error| {
                        panic!("{label}::{name}::{:?}: {error}", entry.name)
                    });
                    stats.scripts += 1;
                    stats.script_bytes += script.bytecode.len();
                } else if entry.name.starts_with(b"sh/rom/") && entry.name.ends_with(b".bin") {
                    let room = RoomResource::decode(entry.data).unwrap_or_else(|error| {
                        panic!("{label}::{name}::{:?}: {error}", entry.name)
                    });
                    stats.rooms += 1;
                    stats.room_objects += room.objects.len();
                }
            }
        }
    }
}

#[test]
fn every_corpus_ink_script_and_room_has_valid_structure() {
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
        stats.scripts >= 20_000,
        "parsed only {} script occurrences",
        stats.scripts
    );
    assert!(
        stats.rooms >= 2_000,
        "parsed only {} room occurrences",
        stats.rooms
    );
    assert!(
        stats.script_bytes >= 1_000_000,
        "only {} script bytes",
        stats.script_bytes
    );
    assert!(
        stats.room_objects >= 10_000,
        "only {} room objects",
        stats.room_objects
    );
}
