package com.connecthub.common.config;

import com.connecthub.modules.features.user.enums.RoleName;
import com.connecthub.modules.features.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Profile("dev")
@RequiredArgsConstructor
@Component
public class InitData  implements CommandLineRunner {
    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        initRoles();
    }

    private void initRoles() {
        if (roleRepository.count() == 0) {
            roleRepository.saveAll(List.of(
                    com.connecthub.modules.features.user.entity.Role.builder().id(RoleName.ROLE_ADMIN.name()).name(RoleName.ROLE_ADMIN).build(),
                    com.connecthub.modules.features.user.entity.Role.builder().id(RoleName.ROLE_USER.name()).name(RoleName.ROLE_USER).build()
            ));
            log.info("Initialized roles");
        }else {
            log.info("Roles already initialized");
        }
    }
}
