## GitHub Actions and Package deployment process

This repo is equipped with a fully one-click deployment process.

### How to use

``.github/workflows/publish.yml`` is the key to how this works.

It has 3 variables: 
 * ``publish``: defaults to ``No``, needs to be ``Yes`` to publish and commit changes; Else will do a 'dry-run'.
 * ``releaseVersion``: defaults to next 'minor' release i.e. ``1.0.5``, edit accordingly based on [Semantic Versioning](https://semver.org/).
 * ``developmentVersion``: defaults to next 'minor' SNAPSHOT version i.e. ``1.0.6-SNAPSHOT``, edit accordingly based on [Semantic Versioning](https://semver.org/).

If you have the correct permissions, you can kick of a version deployment by going to https://github.com/qld-gov-au/kiteworks-integration/actions/workflows/publish.yml
and using the 'workflow_dispatch' Run the workflow by clicking ``Run workflow`` green button once you have altered the vairables above to your liking

### Nexus cache routing setup

If you use Sonatype's Nexus repository manager, you may want to add proxy entries to your Nexus configuration for the GitHub repositories.
Here are a few notes to keep in mind:

GitHub's repositories don't contain indices so do set it high in your loop and ensure ``Not found cache enabled`` is disabled

Nexus should still be able to access specific artifact, pom and checksum file URLs despite the missing directory listings.

It may look like it is not working due to it not pre-fetching, it will only show artifacts that it has 'cached' via Nexus lookup.


Below has been tested to work when creating a github package manager link
* ``Remote Storage`` https://maven.pkg.github.com/${org}/${repo}/ i.e. https://maven.pkg.github.com/qld-gov-au/seleniumHelper/
* ``Online`` ticked
* ``Layout policy:`` Permissive
* ``Not found cache enabled:`` **un-ticked**

* ``Strict Content Type Validation:`` optional: un-ticked

* Create a service GitHub account, generate an access key that is only set to ``package: read``
* Inside Nexus: Authentication: username (GitHub username) password (access key generated)
* ``Use pre-emptive authentication`` ticked
* ``Enable circular redirects:`` ticked
* ``Enable Cookies:`` ticked

You may want to use the more advanced features such as Routing Rule to limit the group id's that would go to your repo's.
Reason being is that it will only provide answers for the versions you have published and nothing more.


### How to reproduce / set up

Ensure these secrets are set up on your repo:

Secrets:
* ``DEPLOY_KEY``: private ssh key that is associated with your service worker.
* ``GPG_EMAIL``: the email address against the GPG cert.
* ``GPG_KEY``: the base64 encoded private key without passphrase.
  
   you can set one if you wish and configure an additional steps to unlock it on usage .
* ``GPG_KEY_ID``: The unique id of the GPG key.

Variables:
* ``GPG_NAME``: The name assicated with the GPG key.

Copy the .github folder to your own repository.
 - Update dependabot 'reviewers'

In your pom.xml files, update to match your org and repo.

In this example the org is ``qld-gov-au`` and the repo is ``kiteworks-integration``
```xml
    <scm>
        <connection>scm:git:${project.scm.url}</connection>
        <developerConnection>scm:git:${project.scm.url}</developerConnection>
        <url>git@github.com:qld-gov-au/seleniumHelper.git</url>
        <tag>HEAD</tag>
    </scm>

    <distributionManagement>
        <repository>
            <id>github</id>
            <name>GitHub Packages</name>
            <url>https://maven.pkg.github.com/qld-gov-au/seleniumHelper</url>
        </repository>
        <snapshotRepository>
            <id>github</id>
            <name>GitHub Packages</name>
            <url>https://maven.pkg.github.com/qld-gov-au/seleniumHelper</url>
        </snapshotRepository>
    </distributionManagement>
```

### Manual for maven central

mvn release:prepare -Prelease 
mvn release:perform -Prelease 
mvn release:clean 

or

mvn deploy -Prelease
