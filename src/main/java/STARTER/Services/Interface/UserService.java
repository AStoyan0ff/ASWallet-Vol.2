package STARTER.Services.Interface;

import STARTER.DTOs.ChangePasswordRequest;
import STARTER.DTOs.DeleteAccountRequest;
import STARTER.DTOs.UserDTO;
import STARTER.DTOs.UserViewDTO;

import java.util.UUID;

public interface UserService {

    void register(UserDTO userDTO);

    UserViewDTO findById(UUID id);
    UserViewDTO findByUsername(String username);

    void changePassword(String username, ChangePasswordRequest request);
    void deleteAccount(String username, DeleteAccountRequest request);
}
