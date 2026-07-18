package vlog.format

import org.scalatest.funsuite.AnyFunSuite

class ObfuscationTest extends AnyFunSuite:

  test("round-trips arbitrary bytes through encode/decode") {
    val plain = "hello, vlog!".getBytes("UTF-8")
    val encoded = Obfuscation.encode(plain, 0x5a.toByte)
    assert(Obfuscation.isObfuscated(encoded))
    assert(Obfuscation.decode(encoded).sameElements(plain))
  }

  test("passes through bytes unchanged when no !VCSK header is present") {
    val plain = "not obfuscated".getBytes("UTF-8")
    assert(!Obfuscation.isObfuscated(plain))
    assert(Obfuscation.decode(plain).sameElements(plain))
  }
