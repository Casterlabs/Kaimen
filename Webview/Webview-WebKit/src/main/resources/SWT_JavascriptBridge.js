function setupComms() {
	// Setup the Bridge.
	const eventHandler = new EventHandler();
	const queryQueue = [];

	function sendToParent(emission) {
		const payload = {
			type: "emission",
			data: emission
		};

		queryQueue.push(JSON.stringify(payload));
	}

	return {
		internal__sendToParent: sendToParent,

		internal__wkGetQueryQueue() {
			const result = JSON.stringify(queryQueue);
			queryQueue.length = 0;
			return result;
		},

		emit(type, data = {}) {
			sendToParent({
				type: type,
				data: data
			});
		},

		...eventHandler
	};

}