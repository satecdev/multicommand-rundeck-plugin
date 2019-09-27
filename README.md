# Rundeck SSH Plugin

This plugin allows you to execute multiple commands in a host jumping thru n hosts (under a single session), in the context of Rundeck command or job step. With stock plugins, the entire command list is first copied to the destination host before executing them. Instead, this plugin opens a SSH connection and pipelines the commands to the destination.
This is specially useful when dealing with routers and other network devices that lack these features.


## Getting Started

These instructions will help you build and install the plugin in a Rundeck server.

### Prerequisites

- Java
- Maven

### Installing

Package plugin with Maven.

Put the plugin file, such as plugin.jar, into the Rundeck server's libext dir:

`cp some-plugin.zip $RDECK_BASE/libext`

The plugin is now enabled, and any providers it defines can be used by nodes.

The Rundeck server does not have to be restarted.


### Annotations

If you want to jump n hosts, you will have to specify, how to jump to that host in a node property called 'ssh-jump-hosts' using
a comma-separated list of nodes.

```json
{
  "@node1": {
    "ssh-password-storage-path": "keys/@userpassword",
    "ssh-jump-hosts": "@node2,@node3",
    "ssh-authentication": "password",
    "hostname": "@hostname1",
    "username": "@username"
  },
  "@node2": {
    "ssh-password-storage-path": "keys/@userpassword",
    "ssh-jump-hosts": "@node3",
    "ssh-authentication": "password",
    "hostname": "@hostname2",
    "username": "@username"
  },
  "@node3": {
    "ssh-password-storage-path": "keys/@userpassword",
    "ssh-jump-hosts": "",
    "ssh-authentication": "password",
    "hostname": "@hostname3",
    "username": "@username"
  }
}
```

If you want to skip the output, you'll have to put the commands inside square brackets.

## Built With


* [Maven](https://maven.apache.org/) - Dependency Management
