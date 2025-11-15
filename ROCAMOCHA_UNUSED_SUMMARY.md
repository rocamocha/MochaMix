# Rocamocha Code - Unused Analysis Summary

**Focus:** Actual mod code only (rocamocha.* domain)  
**Excluded:** Third-party libraries (rm_javazoom, org.rm_yaml)

---

## 📊 Quick Stats

- **Total Unused:** ~419 lines across 4 files + 2 methods
- **Safe to Remove:** ~196 lines (3 items)
- **Needs Evaluation:** ~223 lines (3 items)

---

## ❌ Safe to Remove Immediately

### 1. ZoneUsageGuide.java (169 lines)
**Path:** `v1_21_1/src/main/java/rocamocha/mochamix/zones/ZoneUsageGuide.java`

- Example/documentation code in production source
- Redundant with ZoneManagementExamples.java and README.md
- 0 references in code

### 2. BlockSocket.java (17 lines)
**Path:** `v1_21_1/src/main/java/rocamocha/mochamix/impl/block/BlockSocket.java`

- Unused socket implementation
- 0 imports, 0 instantiations
- Infrastructure that was never adopted

### 3. Vector3Math.sqrt() (~10 lines)
**Path:** `v1_21_1/src/main/java/rocamocha/mochamix/impl/vector3/Vector3Math.java:67`

- Custom square root implementation marked `@SuppressWarnings("unused")`
- Code uses standard `Math.sqrt()` instead
- 0 calls to this method

---

## ⚠️ Evaluate Before Removing

### 4. ZoneManagementExamples.java (178 lines)
**Path:** `v1_21_1/src/main/java/rocamocha/mochamix/zones/ZoneManagementExamples.java`

- Example/documentation code in production source
- 0 references but contains useful examples
- **Recommendation:** Move to `src/test/java` as integration tests

### 5. DamageSourceAdapter.java (35 lines)
**Path:** `v1_21_1/src/main/java/rocamocha/mochamix/impl/entity/adapter/DamageSourceAdapter.java`

- Adapter for damage sources
- 0 imports, 0 instantiations
- Has TODO comment suggesting future use in `LivingEntitySocket.java`
- **Recommendation:** Keep if TODO will be addressed, otherwise remove

### 6. RMPlayer.normalizeSongFileName() (~10 lines)
**Path:** `v1_21_1/src/main/java/rocamocha/reactivemusic/impl/audio/RMPlayer.java:82`

- Song filename normalizer marked `@SuppressWarnings("unused")`
- Comment: "included just in case we need it down the road"
- 0 calls to this method
- **Recommendation:** Remove unless song path normalization is planned

---

## ✅ Everything Else is Used

The rocamocha codebase is **very clean**. All other code is actively used:

- ✅ All 19 plugin classes
- ✅ All mixin accessors (LivingEntityAccess, PlayerEntityAccess, BossBarHudAccessor)
- ✅ All adapter classes (except DamageSourceAdapter)
- ✅ All socket classes (except BlockSocket)
- ✅ NativeAccess (7 usages)
- ✅ Vector3Math (18 usages, all methods except sqrt)
- ✅ Zone management (ZoneData, ZoneDataManager, ZoneUtils, ZoneFactory)

---

## 🎯 Quick Action Plan

### Immediate (15 min)
```bash
rm v1_21_1/src/main/java/rocamocha/mochamix/zones/ZoneUsageGuide.java
rm v1_21_1/src/main/java/rocamocha/mochamix/impl/block/BlockSocket.java
# Edit Vector3Math.java to remove sqrt() method (lines 62-77)
```

### Short-term (30 min)
1. Decide: Move or remove ZoneManagementExamples.java
2. Check if DamageSourceAdapter TODO will be addressed
3. Assess if normalizeSongFileName() is needed

---

## 📝 Key Findings

1. **Rocamocha code is well-maintained** - Only ~419 lines of unused code out of 133 Java files
2. **Most unused code is documentation** - Example classes that should be in tests
3. **Very little abandoned infrastructure** - Just 2 small classes that were never adopted
4. **Previous analysis was misleading** - ~2,500 lines of "unused" code was mostly third-party libraries (which ARE used, just not all parts)

---

## Comparison

| Category | This Analysis | Previous Analysis |
|----------|---------------|-------------------|
| Scope | Rocamocha code only | All code including libraries |
| Total Unused | ~419 lines | ~2,500 lines |
| Files | 4 files + 2 methods | 22 files + methods |
| Focus | Actual mod code | Included rm_javazoom, SnakeYAML |

**Conclusion:** The actual mod code is very clean. The bulk of unused code identified previously was in bundled third-party dependencies, which are kept for functionality (MP3 playback, YAML parsing).
