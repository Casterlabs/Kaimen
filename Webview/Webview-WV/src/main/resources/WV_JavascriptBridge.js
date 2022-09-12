function setupComms() {
    // Setup the Bridge.
    const eventHandler = new EventHandler();

    function sendToParent(payload) {
        __internal_comms("EMIT", payload);
    }

    // Listen for title sets from javascript.
    Object.defineProperty(document, "title", {
        set: (title) => __internal_comms("TITLE", title),
        get: () => "INTERN",
        configurable: true
    });
    
    // Watch for title elements to be added.
    new MutationObserver((mutations) => {
        for (const mutation of mutations) {
            if (mutation.target?.tagName == "TITLE") {
                const text = mutation.target.text;
                __internal_comms("TITLE", text);
            }
        }
	}).observe(document, {attributes: false, childList: true, characterData: false, subtree:true});

    __internal_comms("INIT");

    return {
        internal__eh: eventHandler,
    
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