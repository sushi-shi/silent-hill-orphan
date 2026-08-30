//! Platform-neutral MIDP subset. Hosts own windows, clocks, audio and storage;
//! this crate owns deterministic device semantics and the CPU framebuffer.

extern crate alloc;

use alloc::vec;
use j2me_canvas::Image;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum MeError {
    InvalidDimensions,
    AllocationTooLarge,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct Rect {
    pub x: i32,
    pub y: i32,
    pub width: i32,
    pub height: i32,
}

impl Rect {
    const fn intersect(self, other: Self) -> Self {
        let left = if self.x > other.x { self.x } else { other.x };
        let top = if self.y > other.y { self.y } else { other.y };
        let right = if self.x.saturating_add(self.width) < other.x.saturating_add(other.width) {
            self.x.saturating_add(self.width)
        } else {
            other.x.saturating_add(other.width)
        };
        let bottom = if self.y.saturating_add(self.height) < other.y.saturating_add(other.height) {
            self.y.saturating_add(self.height)
        } else {
            other.y.saturating_add(other.height)
        };
        Self {
            x: left,
            y: top,
            width: right.saturating_sub(left),
            height: bottom.saturating_sub(top),
        }
    }
}

pub struct Framebuffer {
    width: u32,
    height: u32,
    image: Image,
    clip: Rect,
}

impl Framebuffer {
    pub fn new(width: u32, height: u32) -> Result<Self, MeError> {
        if width == 0 || height == 0 || width > i32::MAX as u32 || height > i32::MAX as u32 {
            return Err(MeError::InvalidDimensions);
        }
        let length = width
            .checked_mul(height)
            .and_then(|value| usize::try_from(value).ok())
            .ok_or(MeError::AllocationTooLarge)?;
        let image = Image::from_argb(width as i32, height as i32, vec![0; length])
            .map_err(|_| MeError::AllocationTooLarge)?;
        Ok(Self {
            width,
            height,
            image,
            clip: Rect {
                x: 0,
                y: 0,
                width: width as i32,
                height: height as i32,
            },
        })
    }

    pub const fn width(&self) -> u32 {
        self.width
    }
    pub const fn height(&self) -> u32 {
        self.height
    }
    pub fn pixels(&self) -> &[u32] {
        self.image.pixels()
    }

    pub fn set_clip(&mut self, clip: Rect) {
        self.clip = clip.intersect(Rect {
            x: 0,
            y: 0,
            width: self.width as i32,
            height: self.height as i32,
        });
    }

    pub fn fill_rect(&mut self, rectangle: Rect, argb: u32) {
        let area = rectangle.intersect(self.clip).intersect(Rect {
            x: 0,
            y: 0,
            width: self.width as i32,
            height: self.height as i32,
        });
        if area.width <= 0 || area.height <= 0 {
            return;
        }
        for y in area.y..area.y + area.height {
            for x in area.x..area.x + area.width {
                self.image.set(x, y, argb);
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn fill_is_clipped_on_all_edges() {
        let mut frame = Framebuffer::new(3, 2).unwrap();
        frame.set_clip(Rect {
            x: 1,
            y: 0,
            width: 2,
            height: 2,
        });
        frame.fill_rect(
            Rect {
                x: -2,
                y: -2,
                width: 8,
                height: 8,
            },
            0xff123456,
        );
        assert_eq!(
            frame.pixels(),
            &[0, 0xff123456, 0xff123456, 0, 0xff123456, 0xff123456]
        );
    }
}
