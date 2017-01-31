package ch.fhnw.afpars.workflow

import ch.fhnw.afpars.algorithm.IAlgorithm
import ch.fhnw.afpars.event.Event
import ch.fhnw.afpars.model.AFImage
import ch.fhnw.afpars.ui.controller.ParameterEditView
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

/**
 * Created by cansik on 07.10.16.
 */
class WorkflowEngine {
    val finished = Event<AFImage>()
    val stepDone = Event<Pair<IAlgorithm, AFImage>>()

    private var stepLatch = CountDownLatch(1)

    fun nextStep() {
        stepLatch.countDown()
    }

    fun run(workflow: Workflow, afImage: AFImage, editParameters: Boolean = false, waitAfterStep: Boolean = false) {
        thread {
            var image = afImage
            for (alg in workflow.algorithms) {

                if (editParameters)
                    showEditView(alg, image)

                image = alg.run(image)

                if (waitAfterStep) {
                    stepLatch = CountDownLatch(1)
                    stepDone(Pair(alg, image))
                    stepLatch.await()
                }
            }
            finished(image)
        }
    }

    fun showEditView(algorithm: IAlgorithm, afImage: AFImage) {
        val latch = CountDownLatch(1)

        val fxmlLoader = FXMLLoader(javaClass.classLoader.getResource("view/ParameterEditView.fxml"))
        val root: Parent = fxmlLoader.load()
        val controller = fxmlLoader.getController<ParameterEditView>()
        controller.initView(algorithm, afImage)

        Platform.runLater {
            val stage = Stage()
            stage.title = "Algorithm: ${algorithm.name}"
            stage.scene = Scene(root)
            stage.showAndWait()
            latch.countDown()
        }

        // wait for ui
        latch.await()
    }
}