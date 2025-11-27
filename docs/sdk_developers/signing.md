# Commit Signing Requirements

This document outlines the requirements for signing commits in the Hiero SDK project.

## DCO (Developer Certificate of Origin)

All commits must be signed off using the Developer Certificate of Origin (DCO). This indicates that you have the right to submit the work under the project's license.

### Adding DCO Sign-off

To add a DCO sign-off to your commit, use the `-s` flag:

```bash
git commit -s -m "your commit message"
```

This will automatically add a line like:
```
Signed-off-by: Your Name <your.email@example.com>
```

## GPG Signing

All commits must be GPG signed to ensure authenticity and integrity of the codebase.

### GPG Signing Your Commits

To GPG sign your commits, use the `-S` flag:

```bash
git commit -S -m "your commit message"
```

### Combined DCO and GPG Signing

To fulfill both requirements simultaneously, use both flags:

```bash
git commit -S -s -m "your commit message"
```

## Setting Up GPG Signing

### 1. Generate a GPG Key

If you don't have a GPG key, generate one:

```bash
gpg --full-generate-key
```

### 2. Configure Git to Use Your GPG Key

Find your GPG key ID:
```bash
gpg --list-secret-keys --keyid-format=long
```

Configure Git to use your key:
```bash
git config user.signingkey YOUR_KEY_ID
git config commit.gpgsign true
```

### 3. Upload Your GPG Public Key to GitHub

1. Export your public key:
   ```bash
   gpg --armor --export YOUR_KEY_ID
   ```

2. Go to GitHub Settings > SSH and GPG keys > New GPG key
3. Paste your public key and save

## Verification

You can verify that your commits are properly signed by running:

```bash
git log --show-signature -1
```

This will show the GPG signature verification status for your latest commit.

## Troubleshooting

### "gpg failed to sign the data" Error

If you encounter GPG signing issues:

1. Ensure your GPG key is configured correctly in Git
2. Check that GPG agent is running
3. You may need to set GPG_TTY:
   ```bash
   export GPG_TTY=$(tty)
   ```

### Missing DCO Sign-off

If you forgot to add the DCO sign-off, you can amend your last commit:

```bash
git commit --amend -s
```

## Automated Checks

The project's CI/CD pipeline will automatically verify that:
1. All commits have proper DCO sign-offs
2. All commits are GPG signed

Commits that don't meet these requirements will be rejected automatically.
