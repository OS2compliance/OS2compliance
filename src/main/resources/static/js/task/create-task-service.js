
const createTaskService = new CreateTaskService();
document.addEventListener("DOMContentLoaded", function(event) {
    createTaskService.init();
});

function CreateTaskService() {

    this.init = function() {
        this.load();
    }

    this.load = function() {
        this.loading = true;
        fetch(`/tasks/form`)
            .then(response => response.text()
                .then(data => {
                    document.getElementById('taskFormDialog').innerHTML = data;
                    sharedTaskFormLoaded();
                    initCreateTaskRelationSelect();
                    initTagSelect('createTaskTagsSelect');
                }))
            .catch(error => toastService.error(error))
            .finally(() => this.loading = false);
    }

    this.show = function () {
        if (!this.loading) {

        }
    }
}
