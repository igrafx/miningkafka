# Contributing to the iGrafx Mining Kafka Modules

Thank you for considering contributing to the **iGrafx Mining Kafka Modules**!

All types of contributions are encouraged and valued. See the [Table of Contents](#table-of-contents) for different ways to help and details about how this project handles them.
Please make sure to read the relevant section before making a contribution as it will make it a lot easier for the maintainers
and give an overall better experience to all parties involved. We appreciate your interest in improving the project.

> If you like the project but don't have time to contribute, that's alright.
> There are other easy ways to support the project and show your appreciation, for which we would also be very happy about.
> For instance you can:
> - Star the project
> - Tweet about it, or share it in any way you wish
> - Refer to this project in your project's readme
> - Mention the project at local meetups
> - Tell your friends/colleagues about it

## Table of Contents

- [How Can I Contribute?](#how-can-i-contribute)
- [Code Contribution Guidelines](#code-contribution-guidelines)
- [I have a question](#i-have-a-question)
- [Bug Reports](#bug-reports)
- [Suggesting Enhancements and Features](#suggesting-enhancements-and-features)
- [Submitting a Pull Request](#submitting-a-pull-request)
- [Your First Code Contribution](#your-first-code-contribution)
- [Improving The Documentation](#improving-the-documentation)
- [Styleguide](#styleguide)
- [Git Commit Guidelines](#git-commit-guidelines)
- [License](#license)

## How Can I Contribute?

There are several ways you can contribute to the iGrafx Mining Kafka Modules:

- Reporting bugs or issues
- Requesting new features or enhancements
- Writing documentation
- Submitting code improvements or new features

>Please note that when contributing to this project, you must agree that you have authored 100% of the content and that the content you contribute may be provided under the MIT license.

## Code Contribution Guidelines

When contributing code to the iGrafx Mining Kafka Modules, please adhere to the following guidelines:

1. **Fork** the repository and create your branch from the `dev` branch.
2. Ensure that your code follows the project's **[coding style](#styleguide) and conventions**.
3. Write **clear and concise** [commit messages](#git-commit-guidelines).
4. **Test** your changes thoroughly.
5. Make sure that your code is well-documented, including any new functions, classes, or modules you introduce.
6. Ensure that your code doesn't introduce any linting issues or test failures.

For more details, please consult [this section](#your-first-code-contribution).

## I Have a Question

> If you wish to ask a question, we will assume that you have already read the available [documentation](https://github.com/igrafx/miningkafka/blob/dev/howto.md),
> including the [README.md](https://github.com/igrafx/miningkafka/blob/dev/README.md).

Before asking a question, please search for existing [GitHub Issues](https://github.com/igrafx/miningkafka/issues) that might help you.
If you have found a suitable issue and still need clarification, you can write your question in this issue.
You can also search the internet for answers first.

If you still feel the need to ask a question after doing some research, please do as follows:

- Open an [Issue](https://github.com/igrafx/miningkafka/issues).
- Provide as much context and information as you can about the problem you're having.
- Provide the project and platform versions depending on what seems relevant.
- Do not provide any sort of credentials.

We will then do our best to take care of this issue as soon as possible and get back to you.


## Bug Reports

### Before submitting a bug report

A good bug report shouldn't have others fishing for more information.
That is why we ask you to carefully collect as much information as possible and provide as much detail as possible in your report,
including steps to reproduce the problem.

You can follow the subsequent steps to help us fix any potential bug as fast as possible:

- Make sure you are using the latest version of the iGrafx Mining Kafka Modules.
- Investigate and determine if your bug is really a bug and not an error on your side (e.g. using wrong credentials, outdated dependencies).
- Check that there isn't already a bug report that exists for the one you are having. You can then see if other users have experienced (and potentially solved) the same issue you are having.
- Search the internet (including Stack Overflow) to see if the issue has been discussed outside GitHub.

Afterward, collect information about the bug:

- Stack trace
- Operating system, platform, and versions
- Version of the Scala compiler, JVM, or build tools (e.g., sbt, Maven), depending on what seems relevant.
- Your input and the output
- How the issue could be reproduced

### Submitting a Good Bug Report

> Never report security related issues, vulnerabilities or bugs that include sensitive information (such as credentials) to the issue tracker, or anywhere else in public.
> Instead, sensitive related issues should be sent to [contact@igrafx.com](mailto:contact@igrafx.com).

**GitHub Issues** is used to track bugs and errors, so if you run into an issue within the project, please let us know.:

- Open an [Issue](https://github.com/igrafx/miningkafka/issues). Since we can't be sure it's a bug at this point, don't label the issue.
- Explain the behaviour you expect and the actual behaviour that is observed.
- Provide as much context and information as possible and describe the steps that someone else can follow to recreate the issue on their own.
  This usually includes your code.
- Provide the information you collected in the previous section.

Once the issue is filed:

- Our team will label the issue accordingly.
- We will try to reproduce the issue with the steps you provided.
  If you haven't given the steps to reproduce the bug, we will ask for them and label the issue as ``needs repro``.
- When the team is able to reproduce the issue, it will be labeled `needs fix`, as well as other labels if necessary.
  The list of labels can be found [here](https://github.com/igrafx/miningkafka/labels).
- The issue will then be left to be implemented by someone.

## Suggesting Enhancements and Features

>This section guides you through submitting an enhancement suggestion, including **completely new features** or **minor improvements to an existing functionality**.
>Please follow these guidelines to help everyone involved understand your suggestion.

### Before Submitting a Suggestion

- Make sure that you are using the latest version of the project.
- Read the [documentation](https://github.com/igrafx/miningkafka/blob/dev/howto.md) to find out if the new feature or enhancement has already been covered.
- Perform a search amongst the [issues](https://github.com/igrafx/miningkafka/issues) to see if the enhancement has already been suggested.
  If it has, add a comment to the existing issue instead of opening a new one.
- Find out if your idea fits the scope of the project. Remember that you have to convince the developers of the benefits of this suggestion.
  Keep in mind that it's better to have features that will be useful to the majority of users and not just a few.

### How Do I Submit a Good Enhancement/Feature Suggestion?

>Enhancement suggestions are tracked as [GitHub issues](https://github.com/igrafx/miningkafka/issues).

To submit a good enhancement/feature suggestion, you can follow the subsequent steps:

- Use a **clear and descriptive title** for the issue, to identify and understand the suggestion thoroughly.
- Provide a **step by step description** of the suggested enhancement. The more details, the better.
- Describe the current behaviour and explain which one you expect to see instead and why. You can also explain which alternatives do not fit you.
- If possible, include screenshots or GIFs that help demonstrate the steps or point out the part which the suggestion is related to.
  You can use [this tool](https://www.cockos.com/licecap/) to record GIFs on macOS and Windows
  and [this tool](https://github.com/colinkeenan/silentcast) on Linux.
- Explain why this suggestion would be useful to most users.

## Submitting a Pull Request

Before submitting a pull request, please make sure to:

1. **Fork the repository** and create your branch from the `dev` branch.
2. Make your changes and ensure that they adhere to the [code contribution guidelines](#code-contribution-guidelines).
3. **Test** your changes locally to ensure that they work as expected.
4. Update the relevant [documentation](#improving-the-documentation), including the README and the **howto** files if necessary.
5. Submit the pull request, providing a clear description of the changes you made.

Once your pull request is submitted, it will be reviewed by the project maintainers. Feedback or further changes may be requested. We appreciate your patience during the review process.

## Your First Code Contribution

If you're new to open source or this project, here are some steps to help you get started with your first code contribution:

1. **Fork the repository:** Start by forking this repository to your own GitHub account. This will create a copy of the project under your account.

2. **Clone the repository:** Clone the forked repository to your local machine using a Git client of your choice.

   ```bash
   git clone https://github.com/igrafx/miningkafka.git
   ```
3. **Create a new branch:** Create a new branch on your local machine to work on your contribution.

   ```bash
   git checkout -b my-contribution
   ```
4. **Make changes:** Make the necessary code changes or additions to the project.
5. **Test your changes:** Ensure that your changes are working as expected and do not introduce any errors.
   Add relevant tests and run them to validate your changes.
   The tests are organised in different files. Add the relevant test where you deem it best fits and add comments to describe the test.
   If you feel as though your test doesn't fit in any of those test files, you may create another file.
6. **Commit your changes:** Once you're satisfied with your changes, commit them with a [descriptive commit message](#git-commit-guidelines).
    ```bash
   git commit -m "feat: your feature description"
   ```
7. **Push changes:** Push your changes to your forked repository on GitHub.
    ```bash
   git push origin my-contribution
   ```
8. **Create a pull request:** Go to the original repository on GitHub and create a [pull request](#submitting-a-pull-request).
   Provide a clear and concise description of your changes in the pull request.
9. **Review and collaborate:** Your pull request will be reviewed by the project maintainers. They may provide feedback or ask for further changes.
   Collaborate with them to address any necessary updates.
10. **Celebrate your contribution:** Once your pull request is approved and merged, your contribution will be part of the project!
    Congratulations on your first code contribution!

Please refer to the [Submitting A Pull Request](#submitting-a-pull-request) section for more information about pull requests.

Please note that by submitting a pull request, you agree to license your contribution under the MIT License mentioned in the [LICENSE](https://github.com/igrafx/miningkafka/blob/dev/LICENSE) file.

If you encounter any issues or have suggestions for improvements, please open an issue on the [issue tracker](https://github.com/igrafx/miningkafka/issues). We appreciate your feedback and involvement in improving this project.

Thank you for contributing!

## Improving the Documentation

- When contributing to documentation, use Markdown syntax for formatting. You can refer to the [Markdown Guide](https://www.markdownguide.org/basic-syntax/) for help.
- Use clear and concise language. Write documentation that is easy to understand, providing clear explanations and examples where necessary.
- Keep the documentation up to date. If you make changes to the code that affect the functionality or usage, ensure that the documentation reflects those changes.

## Styleguide

Please follow these styleguides when contributing to the iGrafx Mining Kafka Modules:

- Use consistent indentation. Follow [Scala's official style guide](https://docs.scala-lang.org/style/) to maintain readability and consistency.
- Use meaningful variable and function names. Choose descriptive names that clearly indicate the purpose of the variables, methods, and classes.
- Limit line length. Keep lines of code within 120 characters to ensure readability.
- Use appropriate formatting. Apply `scalafmt` to ensure consistent code formatting.
- Add descriptive comments. Include inline comments to explain complex or non-obvious sections of code, especially when dealing with advanced functional programming techniques or concurrency..
- Ensure your code adheres to the [Scalastyle](http://www.scalastyle.org/) guidelines specified in the project’s `scalastyle-config.xml`.


>These styleguides are in place to maintain code consistency and improve collaboration among contributors.
Please keep them in mind when making contributions to this project.

## Git Commit Guidelines

- Every commit message **must be compliant with [Conventional Commit](https://www.conventionalcommits.org/fr)**.
- Use **descriptive commit messages**. Provide clear and concise commit messages that describe the purpose of the commit.
- Keep commits focused. Make each commit a logical and cohesive unit of work, addressing a single issue or introducing a single feature.
- Reference the issues correctly. If your commit addresses a specific issue, reference it using the issue number in the commit message (e.g. "fix: fix bug #123").
- You can install a [plugin for JetBrains](https://plugins.jetbrains.com/plugin/14046-commitlint-conventional-commit) to
  help to write commit messages.
- BREAKING CHANGE is only used from public API.

## License

By contributing to the iGrafx Mining Kafka Modules, you agree that your contributions will be licensed under the [MIT License](https://github.com/igrafx/miningkafka/blob/dev/LICENSE).

If you have any questions or need further assistance, please contact us at [support@igrafx.com](mailto:support@igrafx.com).

Thank you for contributing!