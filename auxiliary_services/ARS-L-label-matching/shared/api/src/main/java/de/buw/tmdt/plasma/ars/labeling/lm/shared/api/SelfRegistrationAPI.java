package de.buw.tmdt.plasma.ars.labeling.lm.shared.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("")
public interface SelfRegistrationAPI {
    @GetMapping(value = "")
    void heartbeat();
}
