import app
import Observation
import KMPObservableViewModelCore

// One conformance for every production shared feature model.
extension app.ViewModel: @retroactive KMPObservableViewModelCore.ViewModel { }
extension app.ViewModel: @retroactive Observable { }
