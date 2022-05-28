function setupComms() {
	// Setup the Bridge.
	const eventHandler = new EventHandler();

	function sendToParent(emission) {
		const payload = {
			type: "emission",
			data: emission
		};

		__wkinternal_ipc_send(JSON.stringify(payload));
	}

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