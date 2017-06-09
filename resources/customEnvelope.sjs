'use strict';

function validate(contentObj) {
	return {};
}

function transform(context, params, content) {
	var contentObj = content.toObject();
	var type = Object.keys(contentObj)[0];
	contentObj[type]['@type'] = type;
	var newContent = {
		"envelope": {
			"meta": {
				"ingestDateTime": fn.currentDateTime(),
				"ingestUser": xdmp.getCurrentUser(),
				"validation": validate(contentObj)
			},
			"content": contentObj[type]
		}
	};
	var nb = new NodeBuilder();
	nb.startDocument();
	nb.addNode(newContent);
	nb.endDocument();
	return nb.toNode();
};

exports.transform = transform;