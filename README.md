# Damage Source Guard

**Damage Source Guard** is a small stability-focused utility mod for NeoForge 1.21.1.  
It prevents server crashes caused by **invalid or `null` `DamageSource` values** being passed into entity damage or death logic by other mods.

Instead of letting the game crash, this mod applies a safe fallback `DamageSource` and logs detailed warnings to help identify the real cause.

---

## 🔧 What Problem Does This Fix?

In vanilla and in most modded environments, a `DamageSource` **should never be `null`** when an entity is hurt or dies.

However, some mods may:
- manually trigger death logic with `null` damage sources, or
- incorrectly hook into combat / death pipelines,

which can lead to crashes like:

> `NullPointerException: LivingDeathEvent.getSource() is null`

When this happens, **any mod that assumes `getSource()` is non-null may crash the server**, even if it is not the real cause of the bug.

---

## ✅ What This Mod Does

Damage Source Guard installs lightweight safety hooks on:

- `LivingEntity#hurt`
- `LivingEntity#die`

If a `null` `DamageSource` is detected:

1. The mod **replaces it with a safe fallback (`generic`) source**, so the original damage / death still proceeds.
2. A **warning/error is logged**, including:
    - the affected entity and dimension
    - a trimmed stacktrace
    - the first suspicious non-vanilla call site (likely the offending mod)

This prevents crashes **without changing normal gameplay behavior** in valid cases.

---

## ❌ What This Mod Does NOT Do

- ❌ It does **not** hide bugs — it only prevents crashes.
- ❌ It does **not** fix the offending mod internally.
- ❌ It does **not** modify loot tables, AI, or combat mechanics beyond applying a fallback source.

If you see warnings from this mod, it means **another mod is passing invalid data** and should be reported to its author.

---

## 📄 How to Use the Logs

When a bad `DamageSource` is detected, you will see log entries like:

- [DamageSourceGuard] null DamageSource in die. entity=minecraft:zombie pos=... dim=...
- [DamageSourceGuard] suspect=some.mod.Class#method:123
- [DamageSourceGuard] stacktrace:


The `suspect=` line usually points to the mod or system that triggered the invalid call.

You can use this information to:
- report the bug to the correct mod author
- identify incompatibilities in large modpacks

Log output is rate-limited to avoid spam.

---

## 🎯 Why Use This Mod?

This mod is useful if you:

- run large modpacks or servers
- encounter random entity death crashes
- want better diagnostics for combat-related bugs
- need a **non-invasive stability safety net**

It is especially helpful for server operators who cannot easily reproduce rare crash conditions.

---

## ⚙ Compatibility

- ✅ NeoForge **1.21.1**
- ❗ Not needed on the client, but harmless if installed
- Designed to be as minimal and low-risk as possible

---

## 🛠 For Mod Authors

If your mod triggers logs from Damage Source Guard, it likely means:

- `LivingEntity#die(null)` or equivalent is being called, or
- a hook into `hurt()` / `die()` is dropping the original `DamageSource`

Please ensure that all damage and death logic always passes a valid `DamageSource`.

---

## 📜 License (MIT)

MIT License

Copyright (c) 2026 <YOUR NAME>

Permission is hereby granted, free of charge, to any person obtaining a copy  
of this software and associated documentation files (the "Software"), to deal  
in the Software without restriction, including without limitation the rights  
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell  
copies of the Software, and to permit persons to whom the Software is  
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all  
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR  
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,  
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE  
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER  
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,  
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE  
SOFTWARE.
