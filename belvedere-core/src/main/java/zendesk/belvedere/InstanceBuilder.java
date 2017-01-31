package zendesk.belvedere;


import android.content.Context;

interface InstanceBuilder {

    @SuppressWarnings("WeakerAccess")
    class Builder {

        Context context;
        Logger logger;
        boolean debug;
        String directoryName;

        public Builder(Context context) {
            this.context = context;
            this.logger = new DefaultLogger();
            this.debug = false;
            this.directoryName = "belvedere-data-v2";
        }

        public Builder(Context context, Logger logger, boolean debug, String directoryName) {
            this.context = context;
            this.logger = logger;
            this.debug = debug;
            this.directoryName = directoryName;
        }

        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        Builder directoryName(String name) {
            this.directoryName = name;
            return this;
        }

        public Belvedere build() {
            return new Belvedere(this);
        }
    }
}
