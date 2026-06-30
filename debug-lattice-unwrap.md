# Debug Session: lattice-unwrap

- Status: OPEN
- Symptom: 新上传文件下载失败，报错`failed_to_unwrap_lattice_key`
- Expected: 新上传文件应能在上传后立即完成L-ABE解封装并正常下载/预览
- Scope: 文件上传后的L-ABE密钥生成、封装、权威材料读取、用户/系统侧解封装链路

## Hypotheses

1. 新上传文件在`wrap()`时使用的权威公钥与`unwrap()`时读取到的权威私钥不一致，导致Kyber解封装失败。
2. 用户属性密钥材料生成时缓存了旧权威私钥，上传后解封装仍在使用旧bundle。
3. L-ABE策略树里叶子属性或`nodeId`在序列化/反序列化后发生偏差，导致恢复根秘密时取错leaf。
4. `wrap()`与`unwrap()`对AAD或policy字符串的处理不一致，导致最终AES-GCM解密失败并包装成`failed_to_unwrap_lattice_key`。
5. 管理员系统侧解封装和用户侧解封装走了不同密钥来源，其中一条链路读到了错误或被覆盖的权威材料文件。

## Plan

1. 给上传封装、用户解封装、系统解封装三处加调试日志。
2. 复现“新上传后立刻下载失败”并收集运行时证据。
3. 根据证据确认是权威材料、用户bundle、策略树还是AAD不一致。
4. 仅在证据明确后做最小修复。

## Evidence

- Hypothesis 1 rejected:
  - `LatticeAuthorityKeyService:getAuthorityMaterial:existing`日志显示上传封装与解封装阶段读到的`system/person/org/ops/security`权威材料指纹一致，没有发生公私钥漂移。
- Hypothesis 2 rejected:
  - 用户bundle问题不是主因，失败发生在`decode()`阶段，日志在进入真正`recoverSecret()`前就报错。
- Hypothesis 3 confirmed:
  - `LatticeAbeService:unwrapForUser:error`和`LatticeAbeService:unwrapForSystem:error`都显示：
  - `Unrecognized field "leaf" (class com.qar.securitysystem.abe.lattice.LatticePolicyNode)`
  - 说明新上传文件的`policyTree` JSON里包含`leaf`字段，而`LatticePolicyNode`反序列化时不兼容该字段。
- Hypothesis 4 rejected:
  - 修复后日志已显示`rootRecovered: true`，并能继续进入系统侧解封装，说明不是AAD先天不一致导致。
- Hypothesis 5 rejected:
  - 用户侧和系统侧都在相同的`policyTree`反序列化点失败，不是某一侧密钥源单独错误。

## Fix

- 给`LatticePolicyNode`增加了JSON兼容处理：
  - `@JsonIgnoreProperties(ignoreUnknown = true)`
  - 显式兼容历史/冗余字段`leaf`
- 该修复不改动加密算法、权威材料和传输过程，只修复策略树序列化兼容问题。

## Post-Fix Evidence

- 修复后日志已出现：
  - `LatticeAbeService:recoverForSystem:leaf`
  - `LatticeAbeService:unwrapForSystem`中的`rootRecovered: true`
- 这说明系统已经能从同一份新上传密文中成功恢复根秘密，不再在`policyTree`解码阶段失败。
