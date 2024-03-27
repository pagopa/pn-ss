const { handleEvent } = require("./src/app/eventHandler.js");

exports.handler = async (event) => {
  return handleEvent(event);
};
