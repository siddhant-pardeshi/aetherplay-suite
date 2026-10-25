# CI workflow

`ci.yml` is the GitHub Actions workflow for this repository (unit tests + both debug APKs + artifact upload).

It is kept here rather than at `.github/workflows/` because the automation token that
published this repository does not carry the `workflow` scope, which GitHub requires
for pushing files under `.github/workflows/`.

To enable CI, run:

```bash
gh auth refresh -s workflow     # one-time, adds the workflow scope
mkdir -p .github/workflows
git mv ci/ci.yml .github/workflows/ci.yml
git commit -m "ci: enable build workflow" && git push
```
