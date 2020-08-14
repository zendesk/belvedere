package zendesk.belvedere

/**
 * Implementation to allow setting the android version code at runtime in unit tests
 */
class TestBuildVersionProvider: BuildVersionProvider {

    var versionCode = 0

    override fun currentVersion(): Int {
        return versionCode
    }
}