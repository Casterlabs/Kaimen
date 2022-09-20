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
			// CEF has a weird internal way of handling strings, and unfortunately
			// the JNI wrapper mangles them. We've opted to just use URI encoding
			// to prevent manglage.
			request: encodeURIComponent(JSON.stringify(payload)),
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