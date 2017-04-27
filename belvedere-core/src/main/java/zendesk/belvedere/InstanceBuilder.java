package zendesk.belvedere;


import android.content.Context;

interface InstanceBuilder {

    @SuppressWarnings("WeakerAccess")
    class Builder {

        Context context;
        L.Logger logger;
        boolean debug;

        public Builder(Context context) {
            this.context = context;
            this.logger = new L.DefaultLogger();
            this.debug = false;
        }

        public Builder(Context context, L.Logger logger, boolean debug) {
            this.context = context;
            this.logger = logger;
            this.debug = debug;
        }

        public Builder logger(L.Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public Belvedere build() {
            return new Belvedere(this);
        }
    }
}
