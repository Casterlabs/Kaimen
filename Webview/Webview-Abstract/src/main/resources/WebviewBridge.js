if (!window.Bridge) {
    function EventHandler() {
        let listeners = {};
        let callbackIdCounter = 0;

        return {
            on(type, callback) {
                const callbackId = callbackIdCounter++;

                type = type.toLowerCase();

                let callbacks = listeners[type] || {};

                callbacks[callbackId] = callback;

                listeners[type] = callbacks;

                return callbackId;
            },

            once(type, callback) {
                const callbackId = callbackIdCounter++;

                type = type.toLowerCase();

                let callbacks = listeners[type] || {};

                callbacks[callbackId] = function (data) {
                    delete listeners[type][callbackId];
                    callback(data);
                };

                listeners[type] = callbacks;

                return callbackId;
            },

            off(type, callbackId) {
                delete listeners[type][callbackId];
            },

            broadcast(type, data) {
                // Broadcast under a wildcard.
                {
                    const wildCardCallbacks = listeners["*"];

                    if (wildCardCallbacks) {
                        Object.values(wildCardCallbacks).forEach((callback) => {
                            try {
                                callback(type.toLowerCase(), data);
                            } catch (e) {
                                console.error("A listener produced an exception: ");
                                console.error(e);
                            }
                        });
                    }
                }

                // Broadcast under type.
                {
                    const callbacks = listeners[type.toLowerCase()];

                    if (callbacks) {
                        Object.values(callbacks).forEach((callback) => {
                            try {
                                callback(data);
                            } catch (e) {
                                console.error("A listener produced an exception: ");
                                console.error(e);
                            }
                        });
                    }
                }
            }
        };
    }

    function stringifyAndRegisterCallbacks(obj) {
        return JSON.parse(
            JSON.stringify(obj, (k, v) => {
                if (v instanceof Function) {
                    return Bridge.internal__registerCallback(v);
                } else {
                    return v;
                }
            })
        );
    }

    function generateRandomId() {
        return `${Math.random()}${Math.random()}`.split(".").join("");
    }

    "replace with native comms code";

    const comms = setupComms();

    const Bridge = {
        internal__raw: comms,
    
        internal__registerCallback(callback) {
            const callbackId = generateRandomId();

            const id = comms.on(`callback:${callbackId}`, callback);
            const removeHandlerId = comms.on(`callback:${callbackId}:remove`, () => {
                comms.off(`callback:${callbackId}`, id);
                comms.off(`callback:${callbackId}:remove`, removeHandlerId);
            });

            return {
                callbackId: callbackId
            };
        },

        internal__triggermutate(field, data) {
            comms.broadcast(field, data);
        },

        internal__define(path, id) {
            path = path.split(".");
            const propertyName = path.pop();
            let root = window;

            for (const part of path) {
                root = root[part];
            }

            let proxy;

            const object = {
                mutate(field, handler) {
                    const type = `__mutate:${id}:${field}`;
                    proxy[field].then(handler);
                    return [type, comms.on(type, handler)];
                },
                off(id) {
                    try {
                        comms.off(id[0], id[1]);
                    } catch (ignored) { }
                },

                internal__deffun(name) {
                    Object.defineProperty(object, name, {
                        value: function () {
                            const args = Array.from(arguments);

                            return new Promise((resolve, reject) => {
                                const nonce = generateRandomId();

                                comms.once(`_invoke:${nonce}`, ({ isError, result }) => {
                                    if (isError) {
                                        reject(result);
                                    } else {
                                        resolve(result);
                                    }
                                });

                                comms.emit(`_invoke:${id}`, { function: name, args: stringifyAndRegisterCallbacks(args), nonce: nonce });
                            });
                        }
                    });
                },
                internal__defprop(name) {
                    Object.defineProperty(object, name, {
                        value: null,
                        writable: true,
                        configurable: true
                    });
                },
                internal__defraw(name, v) {
                    Object.defineProperty(object, name, {
                        value: v
                    });
                }
            };

            const handler = {
                get(obj, property) {
                    if (typeof obj[property] != "undefined" && obj[property] != null) {
                        return obj[property];
                    }

                    return new Promise((resolve, reject) => {
                        const nonce = generateRandomId();

                        comms.once(`_get:${nonce}`, ({ isError, result }) => {
                            if (isError) {
                                reject(result);
                            } else {
                                resolve(result);
                            }
                        });

                        comms.emit(`_get:${id}`, { property: property, nonce: nonce });
                    });
                },
                set(obj, property, value) {
                    comms.emit(`_set:${id}`, { property: property, value: stringifyAndRegisterCallbacks(value) });
                    // Indicate success
                    return true;
                }
            };

            proxy = new Proxy(object, handler);

            if (typeof root.internal__defraw == "function") {
                root.internal__defraw(propertyName, proxy);
            } else {
                root[propertyName] = proxy;
            }
        },

        ...comms
    };

    Object.freeze(Bridge);
    Object.defineProperty(window, "Bridge", {
        value: Bridge,
        writable: false,
        configurable: false
    });
}
