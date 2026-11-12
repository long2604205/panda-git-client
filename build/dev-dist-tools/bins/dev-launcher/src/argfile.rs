//! The argument file of `java`: the command line of a launch that the host cannot state in one process start.
//!
//! The class path of a distribution names an absolute path per core jar, so a home below a deep output base states a
//! command line longer than Windows starts one. `java` reads the same arguments from the file that an argument with a
//! leading `@` names.
//!
//! The text is written here rather than taken from the `dev-launch` crate, because only this tool starts the JVM
//! itself, and a new dependency edge changes `Cargo.lock` and both Bazel lockfiles.

use std::path::PathBuf;
use std::sync::atomic::{AtomicU64, Ordering};

use anyhow::{Context as _, bail};

/// The number of the argument file that this process has written. See [`temporary_file`].
static SEQUENCE: AtomicU64 = AtomicU64::new(0);

/// The longest command line that this host starts.
///
/// Windows limits one process start to 32767 characters, terminator included. The margin covers the quotes that the
/// host adds. The other hosts pass the arguments and the environment in one block of about two megabytes, which no
/// distribution reaches, so a file there is a last resort that keeps the launch possible.
#[cfg(windows)]
const MAXIMUM_COMMAND_LINE: usize = 32_000;
#[cfg(not(windows))]
const MAXIMUM_COMMAND_LINE: usize = 2 * 1024 * 1024;

/// The arguments of a launch, and the argument file that holds them when the command line is too long.
#[derive(Debug)]
pub(crate) struct Arguments {
    /// The arguments to start with: the launch arguments, or the one argument that names the file.
    pub(crate) values: Vec<String>,
    /// The file that [`remove`] deletes, or `None` when the command line states the arguments itself.
    file: Option<PathBuf>,
}

/// Returns the arguments as one command line where the host starts one that long, and as an argument file otherwise.
pub(crate) fn prepare(program: &str, arguments: &[String]) -> anyhow::Result<Arguments> {
    prepare_with_limit(program, arguments, MAXIMUM_COMMAND_LINE)
}

/// [`prepare`] over a stated limit. A test states a small limit, so it exercises the file on every host.
pub(crate) fn prepare_with_limit(program: &str, arguments: &[String], limit: usize) -> anyhow::Result<Arguments> {
    if command_line_length(program, arguments) <= limit {
        return Ok(Arguments {
            values: arguments.to_vec(),
            file: None,
        });
    }
    let text = argument_file_text(arguments)?;
    let file = temporary_file();
    std::fs::write(&file, text).with_context(|| format!("write the java argument file {}", file.display()))?;
    Ok(Arguments {
        values: vec![format!("@{}", file.display())],
        file: Some(file),
    })
}

/// Deletes the argument file of `arguments`, when it has one.
pub(crate) fn remove(arguments: &Arguments) {
    if let Some(file) = &arguments.file {
        let _ = std::fs::remove_file(file);
    }
}

/// The length of the command line that a process start builds from `program` and `arguments`, with its quotes.
///
/// Every argument counts two quotes and one separator, which is the widest form that the host writes. So a command
/// line that fits this estimate fits the one that the host builds.
pub(crate) fn command_line_length(program: &str, arguments: &[String]) -> usize {
    program.len() + arguments.iter().map(|argument| argument.len() + 3).sum::<usize>()
}

/// The text of a `java` argument file: each argument of `arguments` on its own line, quoted by [`quote_argument`].
pub(crate) fn argument_file_text(arguments: &[String]) -> anyhow::Result<String> {
    let mut text = String::new();
    for argument in arguments {
        // `java` ends an unquoted argument at a line break, so the file would split the argument in silence.
        if argument.contains(['\n', '\r']) {
            bail!("the argument {argument:?} holds a line break, which a java argument file cannot hold");
        }
        text.push_str(&quote_argument(argument));
        text.push('\n');
    }
    Ok(text)
}

/// One argument of a `java` argument file.
///
/// An argument without whitespace, quote, number sign or backslash stays as it is. Every other argument is in double
/// quotes, where a backslash escapes the next character, so each backslash and each double quote is doubled. A number
/// sign starts a comment outside quotes, so an argument that holds one is quoted.
pub(crate) fn quote_argument(argument: &str) -> String {
    let needs_quotes = argument.is_empty()
        || argument.contains(|character: char| character.is_whitespace() || "\"'#\\".contains(character));
    if !needs_quotes {
        return argument.to_owned();
    }
    let mut quoted = String::with_capacity(argument.len() + 2);
    quoted.push('"');
    for character in argument.chars() {
        if character == '"' || character == '\\' {
            quoted.push('\\');
        }
        quoted.push(character);
    }
    quoted.push('"');
    quoted
}

/// A path of the temporary directory that one launch owns.
///
/// The sequence keeps two launches of one process apart, which the tests of this module are. A launch that died before
/// it deleted the file leaves it behind, and the next launch of that number replaces it.
fn temporary_file() -> PathBuf {
    let sequence = SEQUENCE.fetch_add(1, Ordering::Relaxed);
    std::env::temp_dir().join(format!("dev-launcher-{}-{sequence}.args", std::process::id()))
}

#[cfg(test)]
mod tests;
