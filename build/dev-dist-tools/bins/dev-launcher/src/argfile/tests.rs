use super::*;

#[test]
fn a_plain_argument_stays_as_it_is() {
    assert_eq!(quote_argument("com.intellij.idea.Main"), "com.intellij.idea.Main");
    assert_eq!(quote_argument("-Didea.is.internal=true"), "-Didea.is.internal=true");
    assert_eq!(quote_argument("-cp"), "-cp");
}

#[test]
fn an_argument_with_a_space_is_quoted() {
    assert_eq!(
        quote_argument("-Didea.home.path=C:\\Program Files\\IDE"),
        "\"-Didea.home.path=C:\\\\Program Files\\\\IDE\""
    );
}

#[test]
fn a_backslash_and_a_quote_are_doubled_inside_the_quotes() {
    // A Windows class path states one backslash per separator, and a quoted line escapes every one of them.
    assert_eq!(
        quote_argument("C:\\a b\\c.jar;D:\\d e\\f.jar"),
        "\"C:\\\\a b\\\\c.jar;D:\\\\d e\\\\f.jar\""
    );
    assert_eq!(quote_argument("say \"hi\""), "\"say \\\"hi\\\"\"");
}

#[test]
fn a_number_sign_is_quoted_because_it_starts_a_comment() {
    assert_eq!(quote_argument("C:\\ProgramData\\#cache\\a.jar"), "\"C:\\\\ProgramData\\\\#cache\\\\a.jar\"");
}

#[test]
fn an_empty_argument_is_quoted() {
    assert_eq!(quote_argument(""), "\"\"");
}

#[test]
fn the_file_states_one_argument_per_line() {
    let text = argument_file_text(&["-cp".to_owned(), "a b".to_owned(), "Main".to_owned()]).unwrap();
    assert_eq!(text, "-cp\n\"a b\"\nMain\n");
}

#[test]
fn the_file_refuses_an_argument_with_a_line_break() {
    let error = argument_file_text(&["a\nb".to_owned()]).unwrap_err();
    assert!(format!("{error:#}").contains("line break"), "{error:#}");
}

#[test]
fn a_short_command_line_stays_a_command_line() {
    let arguments = ["-cp".to_owned(), "a.jar".to_owned(), "Main".to_owned()];
    let prepared = prepare_with_limit("java", &arguments, 1000).unwrap();
    assert_eq!(prepared.values, arguments);
    assert!(prepared.file.is_none(), "wrote a file for a command line that fits");
}

#[test]
fn a_long_command_line_becomes_a_file_argument() {
    let arguments: Vec<String> = (0..50).map(|index| format!("-Dproperty{index}=a value with spaces")).collect();
    let prepared = prepare_with_limit("java", &arguments, 100).unwrap();
    assert_eq!(prepared.values.len(), 1, "{:?}", prepared.values);
    let file = prepared.file.as_ref().expect("a long command line takes a file");
    assert_eq!(prepared.values[0], format!("@{}", file.display()));
    let text = std::fs::read_to_string(file).unwrap();
    assert_eq!(text, argument_file_text(&arguments).unwrap());
    remove(&prepared);
    assert!(!file.exists(), "kept the argument file");
}

#[test]
fn the_length_counts_a_quote_and_a_separator_per_argument() {
    let arguments = ["ab".to_owned(), "cd".to_owned()];
    assert_eq!(command_line_length("java", &arguments), 4 + (2 + 3) + (2 + 3));
}

#[test]
fn remove_tolerates_a_file_that_is_gone() {
    let prepared = prepare_with_limit("java", &["-cp".to_owned()], 1).unwrap();
    remove(&prepared);
    // The second call reports nothing, because a launch may remove the file before it starts the JVM.
    remove(&prepared);
    assert!(prepare_with_limit("java", &["-cp".to_owned()], 1000).unwrap().file.is_none());
}
