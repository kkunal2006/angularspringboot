package com.example.demo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 *
 * @author kkunal
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusUpdateRequest implements Serializable {

    @NotBlank
    private String status;
}
