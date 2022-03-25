function setupComms() {
    // Setup the Bridge.
    const eventHandler = new EventHandler();

    function sendToParent(payload) {
        __internal_comms(payload);
    }

    __internal_comms("INIT");

    return {
        internal__sendToParent: sendToParent,

        emit(type, data = {}) {
            sendToParent({
                type: type,
                data: data
            });
        },

        ...eventHandler
    };

}