package com.yasser.footballersmarket.worldcup;

import com.yasser.footballersmarket.user.User;
import com.yasser.footballersmarket.worldcup.dto.WorldCupPredictionRequest;
import com.yasser.footballersmarket.worldcup.dto.WorldCupPredictionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/wc/predictions")
public class WorldCupPredictionController {
    private final WorldCupPredictionService predictionService;
    private final Logger logger = LoggerFactory.getLogger(WorldCupPredictionController.class);

    public WorldCupPredictionController(WorldCupPredictionService predictionService) {
        this.predictionService = predictionService;
    }

    // create/update the caller's prediction; rejected once the fixture has kicked off
    @PostMapping
    public ResponseEntity<?> predict(@AuthenticationPrincipal User user,
                                     @Valid @RequestBody WorldCupPredictionRequest req) {
        try {
            predictionService.upsert(user.getId(), req);
            return ResponseEntity.ok().build();
        } catch (PredictionClosedException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error("prediction upsert error user {} fixture {}: {}", user.getId(), req.fixtureId(), e.getMessage());
            return ResponseEntity.internalServerError().body("error occurred");
        }
    }

    // caller's own predictions (visible before kickoff)
    @GetMapping("/mine")
    public List<WorldCupPredictionResponse> myPredictions(@AuthenticationPrincipal User user) {
        return predictionService.getUserPredictions(user.getId());
    }

    // public feed for a fixture: empty until kickoff, then everyone's picks + awarded points
    @GetMapping("/fixture/{fixtureId}")
    public ResponseEntity<?> fixturePredictions(@PathVariable Long fixtureId) {
        try {
            return ResponseEntity.ok(predictionService.getFixturePredictions(fixtureId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
