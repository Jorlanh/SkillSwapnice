package br.com.teamss.skillswap.skill_swap.model.services;

import java.util.List;
import java.util.UUID;

import br.com.teamss.skillswap.skill_swap.model.entities.User;

public interface UserService {
    public List<User> findAll();
    public User findById(UUID id);
    public User save(User user);
    public User addSkills(UUID id, List<Long> skillIds);
    public User addRoles(UUID id, List<Long> roleIds);
    public User update(UUID id, User user);
    public void delete(UUID id);
}
