import MailLogListService from "./MailLogListService.js";

document.addEventListener("DOMContentLoaded", function (event) {
    const mailLogService = new MailLogListService()
    mailLogService.initGrid()
});

