function setupComms() {
	const cefQuery = window.cefQuery;
	const eventHandler = new EventHandler();

	delete window.cefQuery;

	function sendToParent(emission) {
		const payload = {
			type: "emission",
			data: emission
		};

		cefQuery({
			request: JSON.stringify(payload),
			persistent: false,
			onSuccess() { },
			onFailure() { }
		});
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