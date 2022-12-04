function setupComms() {
	const wvPoke = window.wvPoke;
	const wvInit = window.wvInit;
	const eventHandler = new EventHandler();

	delete window.wvPoke;
	delete window.wvInit;
	
	wvInit();

	function sendToParent(emission) {
		const payload = {
			type: "emission",
			data: emission
		};

		wvPoke(payload);
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