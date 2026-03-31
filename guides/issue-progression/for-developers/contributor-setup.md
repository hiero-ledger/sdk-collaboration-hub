# Setting up as a Developer

1. [Setting Up Supporting Infrastructure](#1-setting-up-supporting-infrastructure)  
   - [GitHub Desktop](#github-desktop)  
   - [Visual Studio Code](#visual-studio-code)  

2. [Setting up a Portal Account](#2-setting-up-a-portal-account)  

3. [Set up a GPG key for Signing](#3-set-up-a-gpg-key-for-signing)  

4. [Fork the Repository](#4-fork-the-repository)  

5. [Connect Your Origin With Upstream](#5-connect-your-origin-with-upstream)  

6. [Install Packages and Protobufs](#6-install-packages-and-protobufs)  

7. [Sync Main](#7-sync-main)  

## 1. Setting Up Supporting Infrastructure

For the best development experience and smoother support, we strongly recommend installing:

### Recommended Tools

- [ ] GitHub Desktop  
- [ ] Visual Studio Code  

> These tools are recommendations, not requirements. You are free to use alternatives that fit your workflow.

---

#### GitHub Desktop

GitHub Desktop is a free, user-friendly application that provides a visual interface for Git and GitHub. Instead of running Git commands in a terminal, GitHub Desktop lets you perform common tasks through an intuitive UI.

GitHub Desktop allows you to:
- Clone, fork, and manage repositories without using the command line
- Easily create and switch branches
- Visualize commit history and branches in a clear, interactive timeline
- Stage and push local changes with a click
- Resolve merge conflicts with guided prompts

Overall, GitHub Desktop makes Git simpler, safer, and more visual, which is ideal for maintaining clean pull requests.

---

#### Visual Studio Code

Visual Studio Code (VS Code) is a visual code editor.

It provides:
- Easy project navigation
- Clear file organisation
- Access to a large ecosystem of extensions

---

## 2. Setting up a Portal Account

### Create Testnet Account

Create a testnet Portal account [here](https://portal.hedera.com/dashboard).

Navigate back to github desktop. Then, create a file named `.env` in your project root.

Add, ignoring the < > and without any quotation marks:

```bash
OPERATOR_ID=<YOUR_OPERATOR_ID> #your account id
OPERATOR_KEY=<YOUR_PRIVATE_KEY> #your testnet private key (can be ECDSA, ED25519 or DER)
NETWORK=testnet
```

For example:
```bash
OPERATOR_ID=0.0.1000000
OPERATOR_KEY=123456789
NETWORK=testnet
```

We have added `.env` to `.gitignore` to help ensure its never committed.

## 3. Set up a GPG key for Signing
Follow [Signing Guide](guides/issue-progression/docs/signing.md)

Ensure you later [add the GPG key to Github](https://github.com/settings/gpg/new)

## 4. Fork the Repository

Get a copy of the repository to be able to work on it.

Forking creates a personal, editable version of the SDK under your own GitHub account, where you can safely experiment and prepare pull requests. Once your pull request is ready to review, and once merged, your contribution will be added to the repository.

Make sure you are logged in to GitHub then:
```bash
https://github.com/hiero-ledger/REPOSITORY-NAME
```

Click the top-right button inside the repository `Fork`

GitHub will prompt you to confirm:
- The destination account (your profile)
- The name you want to give your fork (you can keep the default)

Click Create fork.

Your new fork will appear at:
`https://github.com/<your-username>/REPOSITORY-NAME`

This is your copy of the repository. You can work on this safely without fear of impacting the original repository. 

You now have an **online** copy of the repository but you also need a local copy to work on the code.

Using GitHub Desktop (recommended):
1. Open GitHub Desktop.
2. Go to File → Clone Repository
3. Select the fork you just created under the “Your Repositories” tab.
4. Choose a folder location on your machine and click Clone.


## 5. Connect Your Origin With Upstream

# Add upstream for future syncing
```
cd repository-you-forked
git remote add upstream https://github.com/hiero-ledger/repository-to-fork.git
```

for example:
```bash
cd hiero-sdk-python
git remote add upstream https://github.com/hiero-ledger/hiero-sdk-python.git
```

### 6. Install Packages and Protobufs
This installs the package manager uv:
```bash
curl -LsSf https://astral.sh/uv/install.sh | sh
exec $SHELL
```

Now install dependencies as per pyproject.toml:
```bash
uv sync
```

The SDK uses protobuf definitions to generate gRPC and model classes.

Run:
```bash
uv run python generate_proto.py
```

### 7. Sync Main
Always work on an up to date copy of main by following [Rebasing Guide](guides/issue-progression/docs/rebasing.md)
