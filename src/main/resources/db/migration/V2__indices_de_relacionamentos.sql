CREATE INDEX idx_tarefa_projeto_id ON tarefa (projeto_id, id);
CREATE INDEX idx_tarefa_responsavel_id ON tarefa (responsavel_id);
CREATE INDEX idx_projeto_colaboradores_projeto ON projeto_colaboradores (projeto_id);
CREATE INDEX idx_projeto_colaboradores_usuario ON projeto_colaboradores (usuario_id);
