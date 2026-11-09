## Description: <br>
Answers whether a Maven coordinate (dependency, build plugin, version property, BOM import or parent-pinned plugin) is on the newest version by harvesting evidence from POM bytes and each coordinate's own maven-metadata.xml, and can regenerate the dependency-versions audit document through its bundled script pipeline. <br>

This skill is ready for commercial/non-commercial use. <br>

## Third-Party Community Consideration
<span style="color:#d73a49">This skill is not owned or developed by NVIDIA. This skill has been developed and built to a third-party's requirements for this application and use case; see link to Non-NVIDIA [Iván Rodríguez Murillo Agent Card](https://github.com/iromu/awesome-qwen).</span> <!-- VERIFY: Author email in frontmatter is a personal non-NVIDIA address; the repo is a third-party awesome list; card_link is the source repo because no separate vendor card exists. --> <br>

### License/Terms of Use: <br>
Apache 2.0 <br>
## Use Case: <br>
Java/Maven build and release engineers who need ground truth on version currency in a Maven reactor before opening a dependency-upgrade change - which declared coordinates and parent- or BOM-pinned plugin versions are behind, what Maven actually resolved as 'in use' versus what the POM pins, and the exact target version to move to, including profile-gated sub-reactors. <br>

### Deployment Geography for Use: <br>
Global <br>

## Requirements / Dependencies: <br>
**Requires API Key or External Credential:** [Not Specified] <br>
**Credential Type(s):** [None identified] <br>  

Do not include secrets in prompts/logs/output; use least-privilege credentials; rotate keys as appropriate. See skill body for more details. <br>

## Known Risks and Mitigations: <br>
Risk: Review before execution as proposals could introduce incorrect or misleading guidance into skills. <br>
Mitigation: Review and scan skill before deployment. <br>

## Reference(s): <br>
- [requirements.txt (defusedxml pin)](requirements.txt) <br>
- [_va_paths.py (env-overridable path resolution)](scripts/_va_paths.py) <br>
- [_va_xml.py (hardened XML reader, no stdlib fallback)](scripts/_va_xml.py) <br>
- [step1_coords.py (harvest coordinates + pinned versions from POMs)](scripts/step1_coords.py) <br>
- [step2_download.py (fetch maven-metadata.xml per coordinate)](scripts/step2_download.py) <br>
- [step3_doc.py (render the audit document)](scripts/step3_doc.py) <br>
- [verify_ordering.py (self-check: ordering via a 2nd implementation)](scripts/verify_ordering.py) <br>
- [crosscheck.py (self-check: 'behind' verdicts vs the versions plugin)](scripts/crosscheck.py) <br>
- [qa_doc.py (self-check: table shape + no dropped coordinate)](scripts/qa_doc.py) <br>


## Skill Output: <br>
**Output Type(s):** [Analysis, Shell commands, Files] <br>
**Output Format:** [Markdown dependency-version audit tables (pinned / in use / release (repo) / latest plain / latest any plus a current-vs-behind verdict) written to the audit document, plus the pipeline and self-check shell commands and a prose verdict naming the exact target version and any lockstep-family caveat] <br>
**Output Parameters:** [1D] <br>
**Other Properties Related to Output:** [None] <br>

## Evaluation Tasks: <br>
Three eval cases in evals/evals.json - report-mode currency for the dev.openfeature SDK and contrib provider family, regenerate-mode audit of Maven build plugins, and the '~/.m2 directory with only .lastUpdated markers' phantom-version trap - each graded by a per-case assertion set (harvest from maven-metadata.xml rather than hand-eyeballing POMs, report all three version columns, honour the lockstep-family rule); alongside 9 automated SkillEvaluator validator checks (all 9 passed), which is where the 82.0/100 B-grade score comes from. <br>

## Evaluation Metrics Used: <br>
Reported benchmark dimensions: <br>
- Correctness: Whether the harvested version data is machine-harvested and content-verified - coordinates and pins parsed from POM bytes, 'latest' taken from the coordinate's own maven-metadata.xml with the groupId/artifactId checked - rather than reconstructed by hand; scored lowest (70.0) here, with the report's script-level deductions (undocumented scripts, possible missing error handling in _va_paths.py and qa_doc.py) feeding this dimension. <br>
- Discoverability: Whether the skill triggers on any question about version currency of a Maven coordinate even when the user never says 'audit' or 'check', and stays out of npm/package-lock work, adding new dependencies or modules, release notes and changelogs, build failures and test runs. <br>
- Reliability: Whether the pipeline and the three self-checks (verify_ordering.py, crosscheck.py, qa_doc.py) return consistent verdicts across repeat runs - for example not concluding 'nothing to do' from display-property-updates alone, and not treating a local cache listing as proof a version is published. <br>
- Efficiency: Whether the audit completes without unnecessary steps or context overhead, including whether running the whole pipeline to answer a single-coordinate question is justified. <br>



## Evaluation Results: <br>
| Dimension | Score | Grade | Type |
|---|---:|---|---|
| Overall quality | 82.0 | B | script-based |
| Correctness | 70.0 | — | — |
| Discoverability | 90.0 | — | — |
| Reliability | 80.0 | — | — |
| Efficiency | 100.0 | — | — |

## Skill Version(s): <br>
1.0.0 (source: frontmatter) <br>


