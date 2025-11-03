let token = document.getElementsByName("_csrf")[0].getAttribute("content");

document.addEventListener('click', async function(e) {
    if (e.target && e.target.id === 'saveAndContinueBtn') {
        e.preventDefault();
        e.stopPropagation();

        // Find the form within the modal
        let form = e.target.closest('.modal-content').querySelector('form');

        const fd = new FormData(form);

        if (!form) {
            alert('Form not found');
            return;
        }

        // Validate form first
        if (!form.checkValidity()) {
            form.classList.add('was-validated');
            alert('Udfyld venligst alle påkrævede felter');
            return;
        }

        try {
            const formId = form.id;

            const task = {
                id: fd.get('id') || null,
                name: fd.get('name') || '',
                taskType: fd.get('taskType') || null,
                nextDeadline: fd.get(form.id + 'TaskDeadline') || fd.get('nextDeadline') || '',
                responsibleUser: fd.get('responsibleUser') ? { uuid: fd.get('responsibleUser') } : null,
                responsibleOu: fd.get('responsibleOu') ? { uuid: fd.get('responsibleOu') } : null,
                department: fd.get('department') ? { uuid: fd.get('department') } : null,
                repetition: fd.get('repetition') || '',
                description: fd.get('description') || '',
                notifyResponsible: fd.get('notifyResponsible') === 'on' || fd.get('notifyResponsible') === 'true',
                includeInReport: fd.get('includeInReport') === 'on' || fd.get('includeInReport') === 'true',
                tags: (fd.getAll('tags') || []).map(v => ({ id: parseInt(v) })),
                links: []
            };

            const linkInputs = form.querySelectorAll('#linksEditContainer input[type="text"]');
            linkInputs.forEach(input => {
                if (input.value.trim()) task.links.push({ url: input.value.trim() });
            });

            const relations = (fd.getAll('relations') || []).map(v => parseInt(v));
            const taskRiskId = fd.get(form.id + 'TaskRiskId') ? parseInt(fd.get(form.id + 'TaskRiskId')) : null;
            const riskCustomId = fd.get(form.id + 'RiskCustomId') ? parseInt(fd.get(form.id + 'RiskCustomId')) : null;
            const riskCatalogIdentifier = fd.get(form.id + 'RiskCatalogIdentifier') || null;

            // Build request
            const data = {
                task: task,
                relations: relations,
                taskRiskId: taskRiskId,
                riskCustomId: riskCustomId,
                riskCatalogIdentifier: riskCatalogIdentifier
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
                throw new Error('Kunne ikke oprette opgave: ' + response.status);
            }

            // Reset form to create another task
            form.reset();
        } catch (error) {
            console.error('Error creating task:', error);
        }
    }
});
