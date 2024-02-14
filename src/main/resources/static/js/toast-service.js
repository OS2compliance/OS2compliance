
const toastService = new ToastService();

function ToastService() {

    this.error = function(message) {
        const container = document.getElementById('toastContainer');
        const template = document.getElementById('toastTemplate');
        const toastElem = template.cloneNode(true);
        toastElem.querySelector("rect").setAttribute("fill", "#df5645");
        toastElem.querySelector("strong").innerHTML = "Fejl";
        toastElem.querySelector(".toast-body").innerHTML = message;
        container.appendChild(toastElem);
        let toast = bootstrap.Toast.getOrCreateInstance(toastElem);
        toast.show();
        toastElem.addEventListener('hidden.bs.toast', function () {
            container.removeChild(toastElem);
        })
    }

    this.info = function(header, message) {
        const container = document.getElementById('toastContainer');
        const template = document.getElementById('toastTemplate');
        const toastElem = template.cloneNode(true);
        toastElem.querySelector("rect").setAttribute("fill", "#9FCC2E");
        toastElem.querySelector("strong").innerHTML = header;
        if (message) {
            toastElem.querySelector(".toast-body").innerHTML = message;
        }
        container.appendChild(toastElem);
        let toast = bootstrap.Toast.getOrCreateInstance(toastElem);
        toast.show();
        toastElem.addEventListener('hidden.bs.toast', function () {
            container.removeChild(toastElem);
        })
    }

}
