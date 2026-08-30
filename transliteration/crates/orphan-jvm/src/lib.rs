//! Narrow JVM semantic helpers. Operations are explicit so normal Rust
//! arithmetic cannot accidentally replace Java's wrapping and shift rules.

extern crate alloc;

use alloc::vec::Vec;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct ArithmeticException;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct NullPointerException;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct ClassCastException;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct ArrayIndexOutOfBoundsException {
    pub index: i32,
    pub length: i32,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct NegativeArraySizeException {
    pub length: i32,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum ArrayAccessException {
    NullPointer(NullPointerException),
    ArrayIndexOutOfBounds(ArrayIndexOutOfBoundsException),
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum ArrayCopyException {
    NullPointer,
    IndexOutOfBounds,
}

pub fn new_i32_array(length: i32) -> Result<Vec<i32>, NegativeArraySizeException> {
    match j2me_jvm::new_i32_array(length) {
        Ok(values) => Ok(values),
        Err(j2me_jvm::JavaError::NegativeArraySize { length }) => {
            Err(NegativeArraySizeException { length })
        }
        Err(_) => unreachable!("new_i32_array has one closed error variant"),
    }
}

pub fn array_mut<T>(values: Option<&mut [T]>, index: i32) -> Result<&mut T, ArrayAccessException> {
    let values = values.ok_or(ArrayAccessException::NullPointer(NullPointerException))?;
    let length = values.len() as i32;
    if index < 0 || index >= length {
        Err(ArrayAccessException::ArrayIndexOutOfBounds(
            ArrayIndexOutOfBoundsException { index, length },
        ))
    } else {
        Ok(&mut values[index as usize])
    }
}

pub fn array_ref<T>(values: Option<&[T]>, index: i32) -> Result<&T, ArrayAccessException> {
    let values = values.ok_or(ArrayAccessException::NullPointer(NullPointerException))?;
    let length = values.len() as i32;
    if index < 0 || index >= length {
        Err(ArrayAccessException::ArrayIndexOutOfBounds(
            ArrayIndexOutOfBoundsException { index, length },
        ))
    } else {
        Ok(&values[index as usize])
    }
}

pub const fn i32_add(left: i32, right: i32) -> i32 {
    j2me_jvm::i32_add(left, right)
}

pub const fn i32_sub(left: i32, right: i32) -> i32 {
    j2me_jvm::i32_sub(left, right)
}

pub const fn i32_mul(left: i32, right: i32) -> i32 {
    j2me_jvm::i32_mul(left, right)
}

pub const fn i32_div(left: i32, right: i32) -> Result<i32, ArithmeticException> {
    if right == 0 {
        Err(ArithmeticException)
    } else if left == i32::MIN && right == -1 {
        Ok(i32::MIN)
    } else {
        Ok(left / right)
    }
}

pub const fn i32_rem(left: i32, right: i32) -> Result<i32, ArithmeticException> {
    if right == 0 {
        Err(ArithmeticException)
    } else if left == i32::MIN && right == -1 {
        Ok(0)
    } else {
        Ok(left % right)
    }
}

pub const fn i32_shl(value: i32, distance: i32) -> i32 {
    j2me_jvm::i32_shl(value, distance)
}

pub const fn i32_shr(value: i32, distance: i32) -> i32 {
    j2me_jvm::i32_shr(value, distance)
}

pub const fn i32_ushr(value: i32, distance: i32) -> i32 {
    j2me_jvm::i32_ushr(value, distance)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn java_edges_are_explicit() {
        assert_eq!(i32_add(i32::MAX, 1), i32::MIN);
        assert_eq!(i32_div(i32::MIN, -1), Ok(i32::MIN));
        assert_eq!(i32_div(1, 0), Err(ArithmeticException));
        assert_eq!(i32_ushr(-1, 1), i32::MAX);
        assert_eq!(i32_shl(1, 32), 1);
        assert_eq!(
            new_i32_array(-1),
            Err(NegativeArraySizeException { length: -1 })
        );
        assert_eq!(new_i32_array(3), Ok(vec![0, 0, 0]));
        let mut values = [4, 5];
        *array_mut(Some(&mut values), 1).expect("index one exists") = 9;
        assert_eq!(values, [4, 9]);
        assert_eq!(
            array_mut(Some(&mut values), -1),
            Err(ArrayAccessException::ArrayIndexOutOfBounds(
                ArrayIndexOutOfBoundsException {
                    index: -1,
                    length: 2,
                }
            ))
        );
        assert_eq!(
            array_mut::<i32>(None, 0),
            Err(ArrayAccessException::NullPointer(NullPointerException))
        );
        assert_eq!(array_ref(Some(&values), 1), Ok(&9));
        assert_eq!(
            array_ref::<i32>(None, 0),
            Err(ArrayAccessException::NullPointer(NullPointerException))
        );
    }
}
