import {refreshTaskGrid} from './task-center.js';

let token = document.getElementsByName("_csrf")[0].getAttribute("content");

document.addEventListener('click', async function(e) {
    if (e.target && e.target.id === 'saveAndContinueBtn') {
        e.preventDefault();
        e.stopPropagation();

        // Find the form within the modal
        let form = e.target.closest('.modal-content').querySelector('form');

        if (!form) {
            console.error("form not found");
            return;
        }

        const fd = new FormData(form);

        // Validate form first
        if (!form.checkValidity()) {
            form.classList.add('was-validated');
            toastService.info("info", "Udfyld venligst alle påkrævede felter")
            return;
        }

        // Create data and send to backend
        await handleSubmit(fd, form);
    }
});

async function handleSubmit(fd, form) {
    try {
        // Create the task object with the form data
        const task = extractTaskFromFormData(fd, form);
        task.links = extractLinksFromForm(form);
        task.subTasks = extractSubTasksFromForm(form);

        const relations = (fd.getAll('relations') || []).map(v => parseInt(v));
        const riskData = extractRiskDataFromForm(fd, form);

        // Build request
        const data = {
            task: task,
            relations: relations,
            ...riskData
        };

        const response = await fetch('/rest/tasks/create', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                'X-CSRF-TOKEN': token
            },
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            const errorText = await response.text();
            console.error('Server error:', errorText);
            toastService.error('Kunne ikke oprette opgaven');
            return;
        }

        // Close the modal
        form.classList.remove('was-validated');

        let modal = form.closest('.modal');
        let bsModal = bootstrap.Modal.getInstance(modal) || new bootstrap.Modal(modal);
        bsModal.hide();

        refreshTaskGrid();

        toastService.info("info", "Opgaven blev gemt");
    } catch (error) {
        toastService.error('Fejl under oprettelse af opgave');
    }
}

function extractTaskFromFormData(fd, form) {
    return {
        id: fd.get('id') || null,
        name: fd.get('name') || '',
        taskType: fd.get('taskType') || null,
        nextDeadline: fd.get(form.id + 'TaskDeadline') || fd.get('nextDeadline') || '',
        responsibleUserUuids: fd.getAll('responsibleUsers') || [],
        responsibleOuUuid: fd.get('responsibleOu') || null,
        departmentUuid: fd.get('department') || null,
        repetition: fd.get('repetition') || null,
        description: fd.get('description') || '',
        notifyResponsible: fd.get('notifyResponsible') === 'on' || fd.get('notifyResponsible') === 'true',
        includeInReport: fd.get('includeInReport') === 'on' || fd.get('includeInReport') === 'true',
        tagIds: (fd.getAll('tags') || []).map(v => parseInt(v)),
        notificationReminders: fd.getAll('notificationReminders') || [],
        taskDescriptionTemplateId: fd.get('templateDescription') ? parseInt(fd.get('templateDescription')) : null,
        links: [],
        subTasks: []
    };
}

function extractLinksFromForm(form) {
    const links = [];
    const linkInputs = form.querySelectorAll('#linksEditContainer input[type="text"]');

    linkInputs.forEach(input => {
        if (input.value.trim()) {
            links.push({ url: input.value.trim() });
        }
    });

    return links;
}

function extractSubTasksFromForm(form) {
    const subTasks = [];
    const subTaskInputs = form.querySelectorAll('#subTasksContainer input[type="text"]');

    subTaskInputs.forEach(input => {
        if (input.value.trim()) {
            subTasks.push({
                name: input.value.trim(),
                completed: false
            });
        }
    });

    return subTasks;
}

function extractRiskDataFromForm(fd, form) {
    return {
        taskRiskId: fd.get(form.id + 'TaskRiskId') ? parseInt(fd.get(form.id + 'TaskRiskId')) : null,
        riskCustomId: fd.get(form.id + 'RiskCustomId') ? parseInt(fd.get(form.id + 'RiskCustomId')) : null,
        riskCatalogIdentifier: fd.get(form.id + 'RiskCatalogIdentifier') || null
    };
}
