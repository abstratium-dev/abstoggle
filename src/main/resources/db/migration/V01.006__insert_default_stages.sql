-- Seed default stages with inheritance chain: dev → test → prod
INSERT INTO T_stage (id, name, description, display_order, parent_stage_id) VALUES
    ('018fa3e4-0000-7000-8000-000000000001', 'prod', 'Production environment', 1, NULL),
    ('018fa3e4-0000-7000-8000-000000000002', 'test', 'Testing/QA environment', 2, '018fa3e4-0000-7000-8000-000000000001'),
    ('018fa3e4-0000-7000-8000-000000000003', 'dev', 'Development environment', 3, '018fa3e4-0000-7000-8000-000000000002');
