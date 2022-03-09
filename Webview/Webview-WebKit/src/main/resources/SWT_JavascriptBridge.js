
if (!window.Bridge) {
	// Dependencies.

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


	// Setup the Bridge.
	var eventHandler = new EventHandler();
	var queryQueue = [];

	function ThrowawayEventHandler() {
		let listeners = {};
		let callbackIdCounter = 0;

		let throwawyCallbackId;

		const instance = {
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
			},

			destroy() {
				eventHandler.off("*", throwawyCallbackId);
			}
		};

		throwawyCallbackId = eventHandler.on("*", (type, data) => {
			instance.broadcast(type, data);
		});

		return instance;
	}


	function sendToParent(emission) {
		const payload = {
			type: "emission",
			data: emission
		};

		queryQueue.push(JSON.stringify(payload));
	}

	function stringifyAndRegisterCallbacks(obj) {
		return JSON.parse(
			JSON.stringify(obj, (k, v) => {
				if (v instanceof Function) {
					return Bridge.internal_registerCallback(v);
				} else {
					return v;
				}
			})
		);
	}

	const Bridge = {
		internal_registerCallback(callback) {
			const callbackId = `${Math.random()}${Math.random()}`.split(".").join("");

			const id = eventHandler.on(`callback:${callbackId}`, callback);
			const removeHandlerId = eventHandler.on(`callback:${callbackId}:remove`, () => {
				eventHandler.off(`callback:${callbackId}`, id);
				eventHandler.off(`callback:${callbackId}:remove`, removeHandlerId);
			});

			return {
				callbackId: callbackId
			};
		},

		internal_define(path, id) {
			path = path.split(".");
			const propertyName = path.pop();
			let root = window;

			for (const part of path) {
				root = root[part];
			}

			let proxy;

			const mutateEventHandler = new EventHandler();
			const object = {
				mutate(field, handler) {
					proxy[field].then(handler);

					return [field, mutateEventHandler.on(field, handler)];
				},
				off(id) {
					mutateEventHandler.off(id[0], id[1]);
				},

				__triggerMutate(field, data) {
					mutateEventHandler.broadcast(field, data);
				},
				__deffun(name) {
					Object.defineProperty(object, name, {
						value: function () {
							const args = Array.from(arguments);

							return new Promise((resolve, reject) => {
								const nonce = `${Math.random()}${Math.random()}`.split(".").join("");

								eventHandler.once(`_invoke:${nonce}`, ({ isError, result }) => {
									if (isError) {
										reject(result);
									} else {
										resolve(result);
									}
								});

								Bridge.emit(`_invoke:${id}`, { function: name, args: stringifyAndRegisterCallbacks(args), nonce: nonce });
							});
						}
					});
				},
				__defraw(name, v) {
					Object.defineProperty(object, name, {
						value: v
					});
				}
			};

			const handler = {
				get(obj, property) {
					if (typeof obj[property] != "undefined") {
						return obj[property];
					}

					return new Promise((resolve, reject) => {
						const nonce = `${Math.random()}${Math.random()}`.split(".").join("");

						eventHandler.once(`_get:${nonce}`, ({ isError, result }) => {
							if (isError) {
								reject(result);
							} else {
								resolve(result);
							}
						});

						Bridge.emit(`_get:${id}`, { property: property, nonce: nonce });
					});
				},
				set(obj, property, value) {
					Bridge.emit(`_set:${id}`, { property: property, value: stringifyAndRegisterCallbacks(value) });
					// Indicate success
					return true;
				}
			};

			proxy = new Proxy(object, handler);

			if (typeof root.__defraw == "function") {
				root.__defraw(propertyName, proxy);
			} else {
				root[propertyName] = proxy;
			}
		},

		clearQueryQueue() {
			if (queryQueue.length > 0) {
				const copy = queryQueue;
				queryQueue = [];

				return JSON.stringify(copy);
			} else {
				return null;
			}
		},

		emit(type, data = {}) {
			sendToParent({
				type: type,
				data: data
			});
		},

		...eventHandler
	};

	Object.freeze(Bridge);
	Object.defineProperty(window, "Bridge", {
		value: Bridge,
		writable: false,
		configurable: false
	});
}