package(default_visibility = ["//visibility:public"])

load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)

gerrit_plugin(
    name = "events-broker",
    srcs = glob(["src/main/java/**/*.java"]),
)

junit_tests(
    name = "events_broker_tests",
    size = "small",
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["events-broker"],
    deps = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":events-broker",
    ],
)
