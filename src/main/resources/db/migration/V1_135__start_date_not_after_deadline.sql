UPDATE tasks SET start_date = next_deadline WHERE start_date > next_deadline;
