## Description: <br>
Run an iterative review–critique–fix loop over real git changes — uncommitted work, a feature branch's changes versus the default branch, or any commit range, PR, or file set the user names. <br>

This skill is ready for commercial/non-commercial use. <br>

## Third-Party Community Consideration
<span style="color:#d73a49">This skill is not owned or developed by NVIDIA. This skill has been developed and built to a third-party's requirements for this application and use case; see link to Non-NVIDIA [Iván Rodríguez Murillo Agent Card](https://github.com/iromu/awesome-qwen).</span> <!-- VERIFY: Author email in the frontmatter is a personal non-NVIDIA address and the repo is a third-party awesome list; card_link is the source repo because no separate vendor card exists. --> <br>

### License/Terms of Use: <br>
Apache 2.0 <br>
## Use Case: <br>
Developers who need a pre-commit or pre-merge quality gate over their own changes: reviewing uncommitted work, a feature branch's diff versus the default branch, or a named commit range, PR, or file set, with an iterative review–critique–fix loop that cites file:line evidence and stops once the build and tests are green. <br>

### Deployment Geography for Use: <br>
Global <br>

## Requirements / Dependencies: <br>
**Requires API Key or External Credential:** [No] <br>
**Credential Type(s):** [None] <br>  

Do not include secrets in prompts/logs/output; use least-privilege credentials; rotate keys as appropriate. See skill body for more details. <br>

## Known Risks and Mitigations: <br>
Risk: Review before execution as proposals could introduce incorrect or misleading guidance into skills. <br>
Mitigation: Review and scan skill before deployment. <br>

## Reference(s): <br>


## Skill Output: <br>
**Output Type(s):** [Analysis, Shell commands, Code] <br>
**Output Format:** [Markdown review report with a cycle table, per-finding file:line evidence, a verdict, and the git commands used to resolve the target; optional corrected code snippets] <br>
**Output Parameters:** [1D] <br>
**Other Properties Related to Output:** [None] <br>

## Evaluation Agents Used: <br>
- Qwen Code agent (`Qwen-thinking`) <br>



## Evaluation Tasks: <br>
3 eval cases in evals/evals.json (uncommitted+fix, branch+scope, review-only), each a pasted diff with a planted bug, run with-skill vs without-skill and graded against 13 assertions by the skill-creator auto_grader. <br>

## Evaluation Metrics Used: <br>
Reported benchmark dimensions: <br>
- Target resolution: Whether the review target is correctly resolved — uncommitted vs whole-branch (merge-base with the default branch) vs a user-named range — and the commit/file scope is stated. <br>
- Finding accuracy: Whether the planted bug is identified with file:line evidence and the right severity, without fabricating files that are not in the repo. <br>
- Scope discipline: Whether review-only requests are honored (no edits) and fixes are surgical and bounded, with a structured report and verdict. <br>



## Evaluation Results: <br>
| Eval | With skill | Baseline |
|---|---:|---:|
| 1 — uncommitted + fix | 100% (4/4) | 75% (3/4) |
| 2 — branch + scope | 100% (5/5) | 40% (2/5) |
| 3 — review-only | 100% (4/4) | 100% (4/4) |
| **Overall** | **100% (13/13)** | **69% (9/13)** |

## Skill Version(s): <br>
2.0.0 (source: frontmatter) <br>


